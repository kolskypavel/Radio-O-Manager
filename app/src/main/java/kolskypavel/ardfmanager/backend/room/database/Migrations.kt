package kolskypavel.ardfmanager.backend.room.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration from version 1 -> 2: change category.length and category.climb from REAL/float to INTEGER
// Strategy: create new table category_new with INTEGER columns, copy data casting floats to integers (truncation),
// drop old table, rename new table back to category, recreate index.

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1) create new table with desired schema (match EventDatabase_Impl.createAllTables)
        db.execSQL("""
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
        """.trimIndent())

        // 2) copy data from old table, converting length/climb using CAST (truncates toward zero)
        db.execSQL("""
            INSERT INTO category_new (id, race_id, name, is_man, max_age, length, climb, `order`, different_properties, race_type, category_band, `limit`, control_points_string)
            SELECT id, race_id, name, is_man, max_age,
                   CAST(length AS INTEGER),
                   CAST(climb AS INTEGER),
                   `order`, different_properties, race_type, category_band, `limit`, control_points_string
            FROM category
        """.trimIndent())

        // 3) drop old table, rename new to original name
        db.execSQL("DROP TABLE category")
        db.execSQL("ALTER TABLE category_new RENAME TO category")

        // 4) recreate indices expected by Room
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_category_name_race_id` ON `category` (`name`, `race_id`)")
    }
}

