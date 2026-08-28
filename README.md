# CamAtt

CamAtt is a local-first employee attendance MVP. A React dashboard manages employees and attendance, Spring Boot owns business data, and a FastAPI/InsightFace service watches the laptop webcam and reports recognized faces.

## Sample Outcomes
<img width="1902" height="869" alt="image" src="https://github.com/user-attachments/assets/859a20a0-77f5-4b4d-8e5c-4f4da775a039" />
<img width="1920" height="872" alt="image" src="https://github.com/user-attachments/assets/3f158725-76c1-4b44-8b9f-69cc04c8e09c" />
<img width="1920" height="874" alt="image" src="https://github.com/user-attachments/assets/ceb890d4-7736-4e90-b794-a6ca365aef4a" />

<img width="1911" height="876" alt="image" src="https://github.com/user-attachments/assets/bf51cf67-fc54-45c9-9c47-d1f03dc100cb" />
<img width="1920" height="880" alt="image" src="https://github.com/user-attachments/assets/b7ec037c-ed88-47b4-80ec-7d89a634a14b" />
<img width="1916" height="870" alt="image" src="https://github.com/user-attachments/assets/f6bc8625-caa6-44e7-b875-38fa20f0593c" />

## Architecture

```text
React :5173  ->  Spring Boot :8080  ->  MySQL :3306
                         |
                         +---------> FastAPI :8000 -> OpenCV webcam
```

Employee photos and attendance snapshots stay under `storage/`. Face embeddings stay under `ai-service/data/`. No cloud service is involved.

## Quick start

The simplest complete setup is Docker Compose (a webcam is easiest when the AI service is run directly on the host):

1. Copy `.env.example` to `.env` if you want to change defaults.
2. Run `docker compose up --build`.
3. Open http://localhost:5173.

For webcam recognition on Windows, run MySQL/backend/frontend with Docker, then run the AI service on the host:

```powershell
cd ai-service
.\setup.ps1
.\.venv\Scripts\python.exe -m uvicorn app.main:app --reload --port 8000
```

The AI service targets Python 3.14. Its NumPy, OpenCV, InsightFace, and ONNX Runtime versions all provide Python 3.14 wheels, so no local C/C++ compilation should be required. The service still starts with a clear dependency status if the AI packages have not been installed yet.

If Windows reports an MSMF `can't grab frame` warning, restart the AI service once and close Camera, Teams, Zoom, or any browser tab currently using the webcam. CamAtt uses the more reliable DirectShow backend on Windows and automatically reconnects after transient frame failures.

## Local development

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

### Backend

Use Java 21. The included Gradle wrapper handles the Gradle installation:

```powershell
cd backend
.\gradlew.bat bootRun
```

The default configuration expects MySQL. For backend-only development without MySQL, use `.\gradlew.bat bootRun --args="--spring.profiles.active=dev"`; the `dev` profile uses an in-memory H2 database.

## Attendance policy

- First valid sighting today: check-in.
- Further frames inside the recognition cooldown: ignored.
- A sighting after `ATTENDANCE_MIN_SESSION_MINUTES`: check-out.
- Later sightings on the same date do not open another session in this MVP.

Defaults are in `backend/src/main/resources/application.yml`.
