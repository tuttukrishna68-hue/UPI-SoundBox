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
                INSTANCE ?: buildDatabase(context.applicationContext, useEncryption).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context, useEncryption: Boolean): SoundboxDatabase {
            val builder = Room.databaseBuilder(context, SoundboxDatabase::class.java, DB_NAME)
                .fallbackToDestructiveMigration()

            if (useEncryption) {
                val keyManager = DatabaseKeyManager(context)
                val passphrase = keyManager.getOrCreatePassphrase()
                val factory = SupportOpenHelperFactory(passphrase)
                builder.openHelperFactory(factory)
            }

            return builder.build()
        }
    }
}

