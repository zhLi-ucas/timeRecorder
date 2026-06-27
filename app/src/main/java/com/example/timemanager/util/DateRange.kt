package com.example.timemanager.util

import java.time.DayOfWeek
import java.time.LocalDate

object DateRange {

    fun currentFor(
        type: com.example.timemanager.viewmodel.ReviewPeriod,
        today: LocalDate
    ): Pair<LocalDate, LocalDate> = when (type) {
        com.example.timemanager.viewmodel.ReviewPeriod.DAY -> today to today
        com.example.timemanager.viewmodel.ReviewPeriod.WEEK -> {
            val monday = today.with(DayOfWeek.MONDAY)
            monday to monday.plusDays(6)
        }
        com.example.timemanager.viewmodel.ReviewPeriod.MONTH ->
            today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
    }

    fun previousFor(
        type: com.example.timemanager.viewmodel.ReviewPeriod,
        today: LocalDate
    ): Pair<LocalDate, LocalDate> {
        val (curStart, curEnd) = currentFor(type, today)
        return when (type) {
            com.example.timemanager.viewmodel.ReviewPeriod.DAY ->
                curStart.minusDays(1) to curEnd.minusDays(1)
            com.example.timemanager.viewmodel.ReviewPeriod.WEEK ->
                curStart.minusWeeks(1) to curEnd.minusWeeks(1)
            com.example.timemanager.viewmodel.ReviewPeriod.MONTH -> {
                val firstOfPrev = curStart.minusMonths(1).withDayOfMonth(1)
                firstOfPrev to firstOfPrev.withDayOfMonth(firstOfPrev.lengthOfMonth())
            }
        }
    }

    fun statsRange(
        range: com.example.timemanager.viewmodel.StatsRange,
        today: LocalDate
    ): Pair<LocalDate, LocalDate> = when (range) {
        com.example.timemanager.viewmodel.StatsRange.TODAY -> today to today
        com.example.timemanager.viewmodel.StatsRange.WEEK -> {
            val monday = today.with(DayOfWeek.MONDAY)
            monday to monday.plusDays(6)
        }
        com.example.timemanager.viewmodel.StatsRange.MONTH ->
            today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
        com.example.timemanager.viewmodel.StatsRange.YEAR ->
            LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
    }
}
