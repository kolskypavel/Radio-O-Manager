package kolskypavel.ardfmanager.backend.room.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration from version 1 -> 2: change category.length and category.climb from REAL/float to INTEGER
// Strategy: create new table category_new with INTEGER columns, copy data casting floats to integers (truncation),
// drop old table, rename new table back to category, recreate index.

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) create new table with desired schema (match EventDatabase_Impl.createAllTables)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `category_new` (
                `id` BLOB NOT NULL,
                `race_id` BLOB NOT NULL,
                `name` TEXT NOT NULL,
                `is_man` INTEGER NOT NULL,
                `max_age` INTEGER,
                `length` INTEGER NOT NULL,
                `climb` INTEGER NOT NULL,
                `order` INTEGER NOT NULL,
                `different_properties` INTEGER NOT NULL,
                `race_type` TEXT,
                `category_band` TEXT,
                `limit` TEXT,
                `control_points_string` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`race_id`) REFERENCES `race`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent()
        )

        // 2) copy data from old table, converting length/climb using CAST (truncates toward zero)
        db.execSQL(
            """
            INSERT INTO category_new (id, race_id, name, is_man, max_age, length, climb, `order`, different_properties, race_type, category_band, `limit`, control_points_string)
            SELECT id, race_id, name, is_man, max_age,
                   CAST(length AS INTEGER),
                   CAST(climb AS INTEGER),
                   `order`, different_properties, race_type, category_band, `limit`, control_points_string
            FROM category
        """.trimIndent()
        )

        // 3) drop old table, rename new to original name
        db.execSQL("DROP TABLE category")
        db.execSQL("ALTER TABLE category_new RENAME TO category")

        // 4) recreate indices expected by Room
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_category_name_race_id` ON `category` (`name`, `race_id`)")

        // 5) add `interval` column to result_service (Duration stored as text). Use default 'PT10S' for existing rows.
        db.execSQL("ALTER TABLE result_service ADD COLUMN `interval` TEXT NOT NULL DEFAULT 'PT10S'")
    }
}

// Migration from version 2 -> 3: drop orig_* columns from `result` table by recreating it without those columns
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table matching the Result entity schema (without orig_check_time, orig_start_time, orig_finish_time)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `result_new` (
                `id` BLOB NOT NULL,
                `race_id` BLOB NOT NULL,
                `competitor_id` BLOB,
                `si_number` INTEGER,
                `card_type` INTEGER NOT NULL,
                `check_time` TEXT,
                `start_time` TEXT,
                `finish_time` TEXT,
                `readout_time` TEXT NOT NULL,
                `automatic_status` INTEGER NOT NULL,
                `result_status` TEXT NOT NULL,
                `points` INTEGER NOT NULL,
                `run_time` TEXT NOT NULL,
                `modified` INTEGER NOT NULL,
                `sent` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`race_id`) REFERENCES `race`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`competitor_id`) REFERENCES `competitor`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent()
        )

        // Copy data from old table into the new table skipping orig_* columns
        db.execSQL(
            """
            INSERT INTO result_new (id, race_id, competitor_id, si_number, card_type, check_time, start_time, finish_time, readout_time, automatic_status, result_status, points, run_time, modified, sent)
            SELECT id, race_id, competitor_id, si_number, card_type, check_time, start_time, finish_time, readout_time, automatic_status, result_status, points, run_time, modified, sent
            FROM result
        """.trimIndent()
        )

        // Replace old table with the new one
        db.execSQL("DROP TABLE result")
        db.execSQL("ALTER TABLE result_new RENAME TO result")

        // 6) add `init` column to result_service (boolean flag stored as INTEGER). Default 0 for existing rows.
        db.execSQL("ALTER TABLE result_service ADD COLUMN `init` INTEGER NOT NULL DEFAULT 0")
    }
}

// Migration from version 3 -> 4: add start list properties to `category` table
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE category ADD COLUMN `start_list_column` INTEGER")
        db.execSQL("ALTER TABLE category ADD COLUMN `start_list_start_time` TEXT")
        db.execSQL("ALTER TABLE category ADD COLUMN `color` INTEGER")
    }
}

// Migration from version 4 -> 5: add start_time_source to `result` table
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE result ADD COLUMN `start_time_source` TEXT NOT NULL DEFAULT 'PUNCHED'")
    }
}


// Migration from version 5 -> 6: add optional start-time rounding settings to each race
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE race ADD COLUMN `start_time_rounding_enabled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE race ADD COLUMN `start_time_rounding_interval` INTEGER NOT NULL DEFAULT 5")
    }
}


// Migration from version 6 -> 7: preserve the original SportIdent START punch time separately
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE result ADD COLUMN `original_start_punch_time` TEXT")
        // Existing unrounded values are the best available source for older results.
        db.execSQL("UPDATE result SET original_start_punch_time = start_time WHERE start_time_source = 'PUNCHED'")
    }
}
