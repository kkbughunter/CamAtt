from contextlib import asynccontextmanager
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
import asyncio

from .camera import CameraWorker
from .config import *
from .face_engine import FaceEngine, decode_image

engine = FaceEngine(DATA_DIR, MATCH_THRESHOLD)
camera = CameraWorker(engine, CAMERA_INDEX, BACKEND_URL, STORAGE_DIR, REPORT_COOLDOWN_SECONDS, PRESENCE_RESET_SECONDS)


@asynccontextmanager
async def lifespan(_: FastAPI):
    yield
    camera.stop()


app = FastAPI(title="CamAtt Face Service", version="1.0.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"], allow_methods=["*"], allow_headers=["*"])


def status_payload() -> dict:
    return {"running": camera.running, "cameraAvailable": camera.camera_available, "aiAvailable": engine.available, "message": camera.message if engine.available else f"AI dependencies unavailable: {engine.error}", "facesSeen": camera.faces_seen, "lastRecognition": camera.last_recognition, "registeredProfiles": engine.profiles}


@app.get("/health")
def health() -> dict: return {"status": "ok", "aiAvailable": engine.available}


@app.get("/status")
def status() -> dict: return status_payload()


@app.post("/faces/register")
async def register_face(employee_id: int = Form(...), photos: list[UploadFile] = File(...)) -> dict:
    if len(photos) < 2 or len(photos) > 3: raise HTTPException(400, "Exactly 2 or 3 photos are required")
    try:
        images = [decode_image(await photo.read()) for photo in photos]
        samples = await asyncio.to_thread(engine.register, employee_id, images)
        return {"registered": True, "samples": samples, "message": "Face profile created"}
    except (ValueError, RuntimeError) as exc: raise HTTPException(422, str(exc)) from exc


@app.post("/camera/start")
def start_camera() -> dict:
    try: camera.start()
    except RuntimeError as exc: raise HTTPException(503, str(exc)) from exc
    return status_payload()


@app.post("/camera/stop")
def stop_camera() -> dict:
    camera.stop(); return status_payload()


@app.get("/camera/stream")
def camera_stream() -> StreamingResponse:
    def frames():
        while camera.running:
            frame = camera.frame()
            if frame: yield b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + frame + b"\r\n"
            else: import time; time.sleep(.08)
    return StreamingResponse(frames(), media_type="multipart/x-mixed-replace; boundary=frame")
