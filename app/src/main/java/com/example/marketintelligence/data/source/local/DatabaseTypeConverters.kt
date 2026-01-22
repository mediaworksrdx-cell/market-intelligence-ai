package com.example.marketintelligence.data.source.local

import androidx.room.TypeConverter
import java.util.Date

class DatabaseTypeConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
