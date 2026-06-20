package com.example.timemanager.data.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class Converters {
    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? =
        epochDay?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun localDateTimeToEpochMillis(dt: LocalDateTime?): Long? =
        dt?.toInstant(ZoneOffset.UTC)?.toEpochMilli()

    @TypeConverter
    fun epochMillisToLocalDateTime(ms: Long?): LocalDateTime? =
        ms?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
}
