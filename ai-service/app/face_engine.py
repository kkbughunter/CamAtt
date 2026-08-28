from __future__ import annotations
from pathlib import Path
from threading import RLock
from typing import Any
import logging

log = logging.getLogger(__name__)

try:
    import cv2
    import numpy as np
    from insightface.app import FaceAnalysis
    IMPORT_ERROR: str | None = None
except Exception as exc:  # The API can still expose a useful health status.
    cv2 = None  # type: ignore
    np = None  # type: ignore
    FaceAnalysis = None  # type: ignore
    IMPORT_ERROR = str(exc)


class FaceEngine:
    def __init__(self, data_dir: Path, threshold: float) -> None:
        self.data_dir = data_dir
        self.threshold = threshold
        self.index_path = data_dir / "face_index.npz"
        self._model: Any = None
        self._ids: list[int] = []
        self._embeddings: Any = None
        self._lock = RLock()
        self.data_dir.mkdir(parents=True, exist_ok=True)
        if IMPORT_ERROR is None:
            self._load_index()

    @property
    def available(self) -> bool:
        return IMPORT_ERROR is None

    @property
    def error(self) -> str | None:
        return IMPORT_ERROR

    @property
    def profiles(self) -> int:
        return len(self._ids)

    def prepare(self) -> None:
        """Load the recognition model before the webcam is opened."""
        self._ensure_model()

    def _ensure_model(self) -> Any:
        if not self.available:
            raise RuntimeError(f"Face dependencies are unavailable: {IMPORT_ERROR}")
        if self._model is None:
            # buffalo_l downloads into ~/.insightface on first use when not cached.
            self._model = FaceAnalysis(name="buffalo_l", providers=["CPUExecutionProvider"])
            self._model.prepare(ctx_id=-1, det_size=(640, 640))
        return self._model

    def faces(self, image: Any) -> list[Any]:
        return self._ensure_model().get(image)

    def register(self, employee_id: int, images: list[Any]) -> int:
        vectors = []
        for image in images:
            found = self.faces(image)
            if len(found) != 1:
                raise ValueError("Each photo must contain exactly one clearly visible face")
            vector = found[0].normed_embedding.astype("float32")
            vectors.append(vector)
        if len(vectors) < 2:
            raise ValueError("At least two usable face photos are required")
        mean = np.mean(np.stack(vectors), axis=0)
        mean = mean / np.linalg.norm(mean)
        with self._lock:
            mapping = {employee: embedding for employee, embedding in zip(self._ids, self._embeddings if self._embeddings is not None else [])}
            mapping[employee_id] = mean.astype("float32")
            self._ids = sorted(mapping)
            self._embeddings = np.stack([mapping[key] for key in self._ids])
            self._save_index()
        return len(vectors)

    def match(self, embedding: Any) -> tuple[int | None, float]:
        with self._lock:
            if not self._ids or self._embeddings is None:
                return None, 0.0
            scores = self._embeddings @ embedding.astype("float32")
            index = int(np.argmax(scores))
            score = float(scores[index])
            return (self._ids[index], score) if score >= self.threshold else (None, score)

    def _load_index(self) -> None:
        if not self.index_path.exists():
            self._embeddings = np.empty((0, 512), dtype="float32")
            return
        try:
            payload = np.load(self.index_path, allow_pickle=False)
            self._ids = payload["employee_ids"].astype(int).tolist()
            self._embeddings = payload["embeddings"].astype("float32")
        except Exception:
            log.exception("Face index could not be loaded; starting with an empty index")
            self._ids, self._embeddings = [], np.empty((0, 512), dtype="float32")

    def _save_index(self) -> None:
        temporary = self.index_path.with_suffix(".tmp.npz")
        np.savez_compressed(temporary, employee_ids=np.asarray(self._ids, dtype="int64"), embeddings=self._embeddings)
        temporary.replace(self.index_path)


def decode_image(content: bytes) -> Any:
    if IMPORT_ERROR is not None:
        raise RuntimeError(f"Face dependencies are unavailable: {IMPORT_ERROR}")
    image = cv2.imdecode(np.frombuffer(content, dtype=np.uint8), cv2.IMREAD_COLOR)
    if image is None:
        raise ValueError("A photo could not be decoded")
    return image
