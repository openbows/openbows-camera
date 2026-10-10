package com.openbows.camera.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fontToString(value: FontChoice): String = value.name

    @TypeConverter
    fun stringToFont(value: String): FontChoice = FontChoice.valueOf(value)

    @TypeConverter
    fun sizeToString(value: FontSizeChoice): String = value.name

    @TypeConverter
    fun stringToSize(value: String): FontSizeChoice = FontSizeChoice.valueOf(value)
}

@Database(
    entities = [WatermarkTemplate::class, PhotoRecord::class, RemarkHistory::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun templateDao(): TemplateDao
    abstract fun photoDao(): PhotoDao
    abstract fun remarkHistoryDao(): RemarkHistoryDao
}
