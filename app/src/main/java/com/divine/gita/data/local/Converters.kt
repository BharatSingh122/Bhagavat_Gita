package com.divine.gita.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

/** Stores List<String> (chalisa verses) as a JSON array string. */
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = JSONArray(value).toString()

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val array = JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }
}
