package com.example.levelup_gamerpractica.data.local

import android.app.Application

class LevelUpGamerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database) }
}