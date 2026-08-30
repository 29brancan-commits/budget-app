package com.example.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object DateUtils {
    val ZONE_ID: ZoneId = ZoneId.systemDefault()

    fun localDateToMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZONE_ID).toInstant().toEpochMilli()
    }

    fun millisToLocalDate(millis: Long): LocalDate {
        return Instant.ofEpochMilli(millis).atZone(ZONE_ID).toLocalDate()
    }

    fun getPaydayOnOrBefore(date: LocalDate, anchor: LocalDate): LocalDate {
        val daysBetween = ChronoUnit.DAYS.between(anchor, date)
        val mod = (daysBetween % 14 + 14) % 14
        return date.minusDays(mod)
    }
}
