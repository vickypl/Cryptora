package com.yourapp.vault

import android.app.Application
import net.sqlcipher.database.SQLiteDatabase

class VaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SQLiteDatabase.loadLibs(this)
    }
}
