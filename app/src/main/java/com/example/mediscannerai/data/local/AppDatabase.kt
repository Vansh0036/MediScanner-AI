package com.example.mediscannerai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ReportEntity::class,
        SavedMedicineEntity::class,
        ReminderMedicineEntity::class,
        IntakeEntity::class,
        ProfileEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reportDao(): ReportDao
    abstract fun savedMedicineDao(): SavedMedicineDao
    abstract fun reminderMedicineDao(): ReminderMedicineDao
    abstract fun intakeDao(): IntakeDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Version 1 -> 2: adds the table for saved medicine searches.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `saved_medicines` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`info` TEXT NOT NULL, " +
                            "`savedAt` INTEGER NOT NULL)"
                )
            }
        }

        // Version 2 -> 3: adds the table for medicine reminders.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminder_medicines` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`amountNote` TEXT NOT NULL, " +
                            "`notes` TEXT NOT NULL, " +
                            "`slotsMask` INTEGER NOT NULL, " +
                            "`daysMask` INTEGER NOT NULL, " +
                            "`startDay` INTEGER, " +
                            "`endDay` INTEGER, " +
                            "`isPaused` INTEGER NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`profileId` INTEGER NOT NULL)"
                )
            }
        }

        // Version 3 -> 4: adds the "taken" log.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `intake_log` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`medicineId` INTEGER NOT NULL, " +
                            "`slot` INTEGER NOT NULL, " +
                            "`day` INTEGER NOT NULL, " +
                            "`takenAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_intake_log_medicineId_slot_day` " +
                            "ON `intake_log` (`medicineId`, `slot`, `day`)"
                )
            }
        }

        // Version 4 -> 5: adds people. Everything saved so far belongs to person 1, "Me".
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profiles` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `profiles` (`id`, `name`, `createdAt`) " +
                            "VALUES (1, 'Me', ${System.currentTimeMillis()})"
                )
                db.execSQL(
                    "ALTER TABLE `reports` ADD COLUMN `profileId` INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mediscanner.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}