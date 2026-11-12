package com.upisoundbox.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [PaymentEntity::class], version = 1, exportSchema = false)
abstract class SoundboxDatabase : RoomDatabase() {
    abstract fun paymentDao(): PaymentDao

    companion object {
        private const val DB_NAME = "soundbox_encrypted.db"

        @Volatile
        private var INSTANCE: SoundboxDatabase? = null

        fun getInstance(context: Context, useEncryption: Boolean = true): SoundboxDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    if (useEncryption) {
                        try {
                            System.loadLibrary("sqlcipher")
                        } catch (_: Throwable) {}
                    }
                    buildDatabase(context.applicationContext, useEncryption)
                } catch (t: Throwable) {
                    // Fallback to standard Room SQLite if SQLCipher or KeyStore fails on newer OS
                    buildDatabase(context.applicationContext, useEncryption = false)
                }.also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, useEncryption: Boolean): SoundboxDatabase {
            val dbName = if (useEncryption) DB_NAME else "soundbox_unencrypted.db"
            // Never silently wipe a merchant's ledger during an app update.
            // Schema changes must ship with an explicit Room migration.
            val builder = Room.databaseBuilder(context, SoundboxDatabase::class.java, dbName)

            if (useEncryption) {
                // Fail this build if encryption cannot be configured. The caller
                // then makes an explicit, separately named fallback database;
                // never create an ordinary SQLite file under the encrypted name.
                val keyManager = DatabaseKeyManager(context)
                val passphrase = keyManager.getOrCreatePassphrase()
                val factory = SupportOpenHelperFactory(passphrase)
                builder.openHelperFactory(factory)
            }

            return builder.build()
        }
    }
}

