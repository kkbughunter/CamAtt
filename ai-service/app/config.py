from pathlib import Path
import os

BASE_DIR = Path(__file__).resolve().parent.parent
DATA_DIR = Path(os.getenv("FACE_DATA_PATH", BASE_DIR / "data")).resolve()
STORAGE_DIR = Path(os.getenv("STORAGE_PATH", BASE_DIR.parent / "storage")).resolve()
BACKEND_URL = os.getenv("BACKEND_URL", "http://localhost:8080/api").rstrip("/")
CAMERA_INDEX = int(os.getenv("CAMERA_INDEX", "0"))
MATCH_THRESHOLD = float(os.getenv("MATCH_THRESHOLD", "0.48"))
REPORT_COOLDOWN_SECONDS = int(os.getenv("REPORT_COOLDOWN_SECONDS", "5"))
PRESENCE_RESET_SECONDS = int(os.getenv("PRESENCE_RESET_SECONDS", "5"))
