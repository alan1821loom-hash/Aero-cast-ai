# Air Quality Prediction Using Machine Learning and Deep Learning
**Final-Year Academic Project • B.Sc. Artificial Intelligence and Data Science**

---

## 1. Project Overview
AeroCast AI is an end-to-end Environmental Intelligence platform that ingests historical atmospheric observations, validates data schemas, performs chronologically consistent preprocessing to prevent data leakage, fits multiple machine learning and deep learning models (Random Forest, Decision Tree, K-Nearest Neighbors, XGBoost, and 1D Convolutional Neural Networks), calculates rigorous evaluation metrics (MAE, MSE, RMSE, R²), and produces multi-horizon pollution forecasts.

The project features:
1. **Interactive Android App (Kotlin & Jetpack Compose)**: Complete standalone mobile client with embedded mathematical inference engines, Room SQLite database, CPCB NAQI and US EPA AQI sub-index calculators, Canvas time-series charts, and alert threshold triggers.
2. **FastAPI Python Backend**: REST API implementation for serving machine learning models, receiving IoT sensor telemetry payloads, and processing datasets.

---

## 2. Machine Learning & Deep Learning Architectures
- **Random Forest Regressor**: Ensemble of randomized decision trees with bootstrap aggregation to minimize forecast variance without overfitting.
- **Decision Tree Regressor**: Greedy recursive partitioning optimizing Mean Squared Error (variance reduction).
- **K-Nearest Neighbors (KNN)**: Instance-based distance-weighted interpolation on standardized features.
- **XGBoost / Gradient Boosted Trees**: Sequential boosting fitting shallow regression trees to pseudo-residuals with shrinkage regularization.
- **1D CNN Time-Series Regressor**: 1D Convolutional temporal filtering sliding over chronological observation sequences to extract diurnal atmospheric patterns.

---

## 3. Running the Python Backend Locally (Windows / VS Code)

### Prerequisites:
- Python 3.10+ installed
- Visual Studio Code

### Windows PowerShell Commands:
```powershell
# 1. Navigate to the backend directory
cd backend

# 2. Create a virtual environment
python -m venv venv

# 3. Activate the virtual environment
.\venv\Scripts\Activate.ps1

# If execution policy blocks script execution, run:
# Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process

# 4. Install dependencies
pip install -r requirements.txt

# 5. Start the FastAPI server
uvicorn app.main:app --reload --port 8000
```

### Accessing Interactive API Documentation (Swagger):
Once running, open your web browser to:
- **Swagger UI**: `http://localhost:8000/docs`
- **ReDoc**: `http://localhost:8000/redoc`

---

## 4. API Endpoints
- `GET /api/health`: Health status & academic project metadata.
- `GET /api/dashboard`: High-level summary of records, models, and alerts.
- `POST /api/datasets/upload`: Upload CSV dataset for parsing & validation.
- `POST /api/models/train`: Train Random Forest, Decision Tree, KNN, and XGBoost models.
- `POST /api/predictions`: Multi-horizon pollutant concentration forecasting.
- `GET /api/alerts`: Active environmental threshold breach logs.
- `POST /api/sensors/readings`: Authenticated IoT sensor payload ingestion endpoint.

---

## 5. IoT Sensor Integration
To connect a real hardware node (e.g., ESP32 with PMS5003 and MQ135):
1. Configure your micro-controller to send HTTP POST requests to `/api/sensors/readings`.
2. Format the JSON payload:
   ```json
   {
     "sensor_id": "AQ-NODE-8082",
     "pollutant": "PM2.5",
     "value": 44.8,
     "timestamp": "2026-10-12T12:00:00Z",
     "api_key": "sec_live_aerocast_demo"
   }
   ```
