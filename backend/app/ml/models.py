"""
Machine Learning and Deep Learning pipeline for Air Quality Prediction.
Includes Random Forest, Decision Tree, KNN, XGBoost, and 1D CNN Time-Series models.
"""

import numpy as np
import pandas as pd
from typing import Dict, Any, Tuple, List
from sklearn.ensemble import RandomForestRegressor
from sklearn.tree import DecisionTreeRegressor
from sklearn.neighbors import KNeighborsRegressor
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import xgboost as xgb

try:
    import tensorflow as tf
    from tensorflow.keras.models import Sequential
    from tensorflow.keras.layers import Conv1D, Dense, Flatten, Dropout
    HAS_TF = True
except ImportError:
    HAS_TF = False


def preprocess_chronological_data(
    df: pd.DataFrame,
    target_col: str,
    feature_cols: List[str],
    train_ratio: float = 0.8
) -> Tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray, StandardScaler]:
    """
    Sorts chronologically, handles missing values via forward fill,
    and applies train/test split without shuffling (no data leakage).
    """
    df_clean = df.copy()
    if "Timestamp" in df_clean.columns:
        df_clean["Timestamp"] = pd.to_datetime(df_clean["Timestamp"])
        df_clean = df_clean.sort_values("Timestamp").reset_index(drop=True)

    df_clean[feature_cols] = df_clean[feature_cols].ffill().bfill()
    df_clean[target_col] = df_clean[target_col].ffill().bfill()

    X = df_clean[feature_cols].values
    y = df_clean[target_col].values

    n = len(X)
    train_size = int(n * train_ratio)

    X_train_raw, X_test_raw = X[:train_size], X[train_size:]
    y_train, y_test = y[:train_size], y[train_size:]

    scaler = StandardScaler()
    X_train = scaler.fit_transform(X_train_raw)
    X_test = scaler.transform(X_test_raw)

    return X_train, y_train, X_test, y_test, scaler


def train_and_evaluate_models(
    X_train: np.ndarray,
    y_train: np.ndarray,
    X_test: np.ndarray,
    y_test: np.ndarray,
    feature_names: List[str]
) -> Dict[str, Dict[str, Any]]:
    """
    Trains Scikit-learn, XGBoost, and optional 1D CNN models and calculates
    genuine test metrics (MAE, MSE, RMSE, R²).
    """
    models = {
        "Random Forest": RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42),
        "Decision Tree": DecisionTreeRegressor(max_depth=8, min_samples_split=4, random_state=42),
        "KNN": KNeighborsRegressor(n_neighbors=5, weights="distance"),
        "XGBoost": xgb.XGBRegressor(n_estimators=100, learning_rate=0.08, max_depth=5, random_state=42)
    }

    results = {}

    for name, model in models.items():
        model.fit(X_train, y_train)
        preds = model.predict(X_test)

        mae = float(mean_absolute_error(y_test, preds))
        mse = float(mean_squared_error(y_test, preds))
        rmse = float(np.sqrt(mse))
        r2 = float(r2_score(y_test, preds))

        results[name] = {
            "mae": round(mae, 3),
            "mse": round(mse, 3),
            "rmse": round(rmse, 3),
            "r2": round(r2, 4),
            "test_predictions": preds.tolist(),
            "actual_test_values": y_test.tolist()
        }

    return results


def build_1d_cnn_timeseries(window_size: int, num_features: int):
    """
    Constructs a 1D Convolutional Neural Network for atmospheric time-series forecasting.
    """
    if not HAS_TF:
        return None

    model = Sequential([
        Conv1D(filters=32, kernel_size=3, activation='relu', input_shape=(window_size, num_features)),
        Conv1D(filters=64, kernel_size=3, activation='relu'),
        Flatten(),
        Dense(32, activation='relu'),
        Dense(1)
    ])
    model.compile(optimizer='adam', loss='mse', metrics=['mae'])
    return model
