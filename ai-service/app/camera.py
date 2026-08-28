from __future__ import annotations
from datetime import datetime
from pathlib import Path
from threading import Event, Lock, Thread
import logging
import os
import time
import requests

from .face_engine import FaceEngine, cv2

log = logging.getLogger(__name__)


class CameraWorker:
    def __init__(self, engine: FaceEngine, camera_index: int, backend_url: str, storage_dir: Path, report_cooldown: int, presence_reset: int = 5) -> None:
        self.engine, self.camera_index, self.backend_url = engine, camera_index, backend_url
        self.storage_dir, self.report_cooldown, self.presence_reset = storage_dir, report_cooldown, presence_reset
        self.running = False; self.camera_available = False; self.message = "Camera is stopped"; self.faces_seen = 0
        self.last_recognition: dict | None = None
        self._stop = Event(); self._thread: Thread | None = None; self._frame: bytes | None = None; self._frame_lock = Lock(); self._last_report: dict[int, float] = {}; self._present: dict[int, float] = {}

    def start(self) -> None:
        if self._thread and self._thread.is_alive():
            if self._stop.is_set():
                raise RuntimeError("The previous camera session is still shutting down; wait a moment and try again")
            return
        if not self.engine.available: raise RuntimeError(self.engine.error or "Face engine is unavailable")
        self._stop.clear(); self.running = True; self.message = "Opening built-in webcam"
        self._thread = Thread(target=self._run, name="camatt-camera", daemon=True); self._thread.start()

    def stop(self) -> None:
        self._stop.set()
        if self._thread and self._thread.is_alive(): self._thread.join(timeout=5)
        if self._thread and self._thread.is_alive():
            self.message = "Camera is finishing its current AI operation"
            return
        self.running = False; self.camera_available = False; self.message = "Camera is stopped"

    def frame(self) -> bytes | None:
        with self._frame_lock: return self._frame

    def _run(self) -> None:
        capture = None
        try:
            self.message = "Loading face recognition model"
            self.engine.prepare()
            if self._stop.is_set(): return
            capture = self._open_capture()
            if not capture.isOpened():
                self.message = f"Could not open camera {self.camera_index}. Close other camera apps and try again"
                return
            self.camera_available = True; self.message = "Watching for registered employees"; frame_number = 0; failed_reads = 0
            while not self._stop.is_set():
                ok, frame = capture.read()
                if not ok:
                    failed_reads += 1
                    if failed_reads < 3:
                        self._stop.wait(.2)
                        continue
                    self.camera_available = False; self.message = "Camera lost; attempting to reconnect"
                    capture.release()
                    if self._stop.wait(1.5): break
                    capture = self._open_capture()
                    failed_reads = 0
                    if not capture.isOpened():
                        capture.release()
                        if self._stop.wait(2): break
                        continue
                    self.camera_available = True; self.message = "Watching for registered employees"
                    continue
                failed_reads = 0
                frame_number += 1
                if frame_number % 4 == 0: self._recognize(frame)
                encoded, buffer = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 78])
                if encoded:
                    with self._frame_lock: self._frame = buffer.tobytes()
        except Exception as exc:
            log.exception("Camera loop stopped")
            self.message = f"Camera error: {exc}"
        finally:
            if capture is not None: capture.release()
            self.camera_available = False; self.running = False

    def _open_capture(self):
        # Media Foundation can lose a laptop webcam after a rapid stop/start.
        # DirectShow is more reliable for long-running capture on Windows.
        if os.name == "nt":
            capture = cv2.VideoCapture(self.camera_index, cv2.CAP_DSHOW)
        else:
            capture = cv2.VideoCapture(self.camera_index)
        if capture.isOpened():
            capture.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
            capture.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
            capture.set(cv2.CAP_PROP_BUFFERSIZE, 1)
        return capture

    def _recognize(self, frame) -> None:
        faces = self.engine.faces(frame); self.faces_seen = len(faces)
        now = time.time()
        matched: list[tuple[int, float]] = []
        for face in faces:
            employee_id, score = self.engine.match(face.normed_embedding)
            if employee_id is None: continue
            matched.append((employee_id, score))

        # A person must leave the camera view before another attendance event is
        # allowed. This prevents continuous frames from becoming a Check-Out.
        matched_ids = {employee_id for employee_id, _ in matched}
        for employee_id in list(self._present):
            if employee_id not in matched_ids and now - self._present[employee_id] >= self.presence_reset:
                del self._present[employee_id]

        for employee_id, score in matched:
            was_present = employee_id in self._present
            self._present[employee_id] = now
            if was_present or now - self._last_report.get(employee_id, 0) < self.report_cooldown: continue
            self._last_report[employee_id] = now
            image_path = self._save_snapshot(frame, employee_id)
            self.last_recognition = {"employeeId": employee_id, "confidence": round(score * 100, 2), "at": datetime.now().isoformat()}
            Thread(target=self._report, args=(employee_id, score, image_path), daemon=True).start()

    def _save_snapshot(self, frame, employee_id: int) -> Path:
        folder = self.storage_dir / "attendance" / datetime.now().strftime("%Y-%m-%d")
        folder.mkdir(parents=True, exist_ok=True)
        path = folder / f"{employee_id}_{datetime.now().strftime('%H%M%S_%f')}.jpg"
        cv2.imwrite(str(path), frame)
        return path

    def _report(self, employee_id: int, score: float, image_path: Path) -> None:
        try:
            response = requests.post(f"{self.backend_url}/attendance/recognitions", json={"employeeId": employee_id, "confidence": round(score * 100, 2), "imagePath": str(image_path)}, timeout=5)
            response.raise_for_status(); result = response.json(); self.message = result.get("message", "Recognition reported")
        except Exception as exc:
            log.warning("Could not report recognition: %s", exc); self.message = "Face recognized; backend is unavailable"
