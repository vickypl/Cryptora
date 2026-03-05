package com.yourapp.vault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [CredentialEntity::class], version = 3, exportSchema = true)
abstract class VaultDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE credentials ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE credentials ADD COLUMN emailLogin TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN emailPassword TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN bankCustomerId TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN bankAccountNo TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN bankIfscCode TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN bankNetLogin TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN bankNetPassword TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN bankAppLogin TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN bankAppPassword TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN cardNumber TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN cardCvv TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN cardExpiry TEXT")
                database.execSQL("ALTER TABLE credentials ADD COLUMN identityId TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS index_credentials_description ON credentials(description)")
            }
        }

        fun build(context: Context, passphrase: ByteArray): VaultDatabase {
            return Room.databaseBuilder(
                context,
                VaultDatabase::class.java,
                "vault_encrypted.db"
            )
                .openHelperFactory(SupportFactory(passphrase))
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
