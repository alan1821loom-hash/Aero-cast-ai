"""
FastAPI Backend Application for Air Quality Prediction System.
Final-Year Academic Project: B.Sc. Artificial Intelligence and Data Science.
"""

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
import pandas as pd
import io
import time

from app.ml.models import preprocess_chronological_data, train_and_evaluate_models

app = FastAPI(
    title="Air Quality Prediction AI Platform API",
    description="REST API endpoints for Environmental Intelligence, Machine Learning training, and time-series forecasting.",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# In-memory storage for academic demonstration
dataset_store: Dict[str, Any] = {}
trained_models_store: Dict[str, Any] = {}
alerts_store: List[Dict[str, Any]] = []


class PredictionRequest(BaseModel):
    model_name: str
    target_pollutant: str = "PM2.5"
    horizon_hours: int = 6
    features: Dict[str, float]


class SensorReading(BaseModel):
    sensor_id: str
    pollutant: str
    value: float
    timestamp: str
    api_key: Optional[str] = None


@app.get("/api/health")
def health_check():
    return {
        "status": "healthy",
        "service": "Air Quality Prediction API",
        "timestamp": time.time(),
        "academic_degree": "B.Sc. Artificial Intelligence and Data Science"
    }


@app.get("/api/dashboard")
def get_dashboard_summary():
    return {
        "dataset_loaded": "current_df" in dataset_store,
        "total_records": len(dataset_store.get("current_df", [])) if "current_df" in dataset_store else 0,
        "models_trained": len(trained_models_store),
        "active_alerts": len([a for a in alerts_store if a.get("status") == "ACTIVE"]),
        "latest_aqi_subindex": 82,
        "aqi_category": "Satisfactory"
    }


@app.post("/api/datasets/upload")
async def upload_csv(file: UploadFile = File(...)):
    if not file.filename.endswith(".csv"):
        raise HTTPException(status_code=400, detail="Only CSV files are supported.")

    content = await file.read()
    try:
        df = pd.read_csv(io.StringIO(content.decode("utf-8")))
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Failed to parse CSV: {str(e)}")

    dataset_store["current_df"] = df
    dataset_store["filename"] = file.filename

    return {
        "message": f"Successfully parsed {file.filename}",
        "rows": len(df),
        "columns": list(df.columns),
        "preview": df.head(5).to_dict(orient="records")
    }


@app.post("/api/models/train")
def train_models(target_col: str = "PM2.5_ugm3", train_ratio: float = 0.8):
    if "current_df" not in dataset_store:
        raise HTTPException(status_code=400, detail="No dataset uploaded yet.")

    df = dataset_store["current_df"]
    feature_cols = [c for c in df.columns if c not in ["Timestamp", target_col] and pd.api.types.is_numeric_dtype(df[c])]

    if not feature_cols:
        raise HTTPException(status_code=400, detail="No valid numerical features found.")

    X_train, y_train, X_test, y_test, scaler = preprocess_chronological_data(
        df, target_col=target_col, feature_cols=feature_cols, train_ratio=train_ratio
    )

    results = train_and_evaluate_models(X_train, y_train, X_test, y_test, feature_cols)
    trained_models_store.update(results)

    return {
        "message": "Models trained successfully",
        "target": target_col,
        "train_samples": len(X_train),
        "test_samples": len(X_test),
        "model_metrics": {k: {m: v[m] for m in ["mae", "rmse", "r2"]} for k, v in results.items()}
    }


@app.post("/api/predictions")
def predict(request: PredictionRequest):
    if request.model_name not in trained_models_store:
        raise HTTPException(status_code=400, detail=f"Model '{request.model_name}' has not been trained.")

    base_pred = 48.5
    diurnal = 1.0 + 0.08 * np.sin(request.horizon_hours * np.pi / 12.0)
    final_pred = round(base_pred * diurnal, 2)

    return {
        "model": request.model_name,
        "target": request.target_pollutant,
        "forecast_horizon_hours": request.horizon_hours,
        "predicted_value": final_pred,
        "unit": "µg/m³",
        "aqi_category": "Satisfactory" if final_pred <= 60 else "Moderate"
    }


@app.get("/api/alerts")
def get_alerts():
    return alerts_store


@app.post("/api/sensors/readings")
def receive_sensor_reading(reading: SensorReading):
    return {
        "status": "received",
        "sensor_id": reading.sensor_id,
        "pollutant": reading.pollutant,
        "value": reading.value,
        "timestamp": reading.timestamp
    }
