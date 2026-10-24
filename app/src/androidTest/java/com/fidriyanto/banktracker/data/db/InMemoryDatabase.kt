package com.fidriyanto.banktracker.data.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry

// A fresh database in memory for one test. It runs the real SQL of the DAOs on the device's SQLite.
fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
