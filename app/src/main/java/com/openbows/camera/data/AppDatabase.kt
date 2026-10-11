/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

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
