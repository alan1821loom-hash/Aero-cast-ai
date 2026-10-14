package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AirQualityDao
import com.example.data.local.entities.AlertEntity
import com.example.data.local.entities.AlertThresholdEntity
import com.example.data.local.entities.DatasetMetadataEntity
import com.example.data.local.entities.PredictionHistoryEntity
import com.example.data.local.entities.SensorLogEntity
import com.example.data.local.entities.TrainedModelEntity

@Database(
    entities = [
        DatasetMetadataEntity::class,
        TrainedModelEntity::class,
        PredictionHistoryEntity::class,
        AlertEntity::class,
        AlertThresholdEntity::class,
        SensorLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun airQualityDao(): AirQualityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "air_quality_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
