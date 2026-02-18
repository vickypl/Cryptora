package com.yourapp.vault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [CredentialEntity::class], version = 1, exportSchema = true)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao

    companion object {
        fun build(context: Context, passphrase: ByteArray): VaultDatabase {
            return Room.databaseBuilder(
                context,
                VaultDatabase::class.java,
                "vault_encrypted.db"
            )
                .openHelperFactory(SupportFactory(passphrase))
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
