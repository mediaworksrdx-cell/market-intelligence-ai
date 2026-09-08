package com.marketintelligence.ai.data.source.local

import androidx.room.TypeConverter
import java.util.Date

// Reverted to a simple class. Room will now be told to use this directly via an annotation on the AppDatabase class.
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
