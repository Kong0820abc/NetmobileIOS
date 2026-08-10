package com.xyz.netmobile

import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

object SubscriptionManager {
    fun calculateNextDueDate(platform: Platform, currentDueDateStr: String?): String {
        val now = try {
            LocalDate.parse(platform.getCurrentDate())
        } catch (e: Exception) {
            LocalDate(2026, 1, 1)
        }
        
        val currentDueDate = try {
            if (!currentDueDateStr.isNullOrEmpty()) LocalDate.parse(currentDueDateStr) else null
        } catch (e: Exception) {
            null
        }

        val resultDate = if (currentDueDate == null || currentDueDate < now) {
            val nextMonth = now.plus(1, DateTimeUnit.MONTH)
            LocalDate(nextMonth.year, nextMonth.month, 6)
        } else {
            val plusMonth = currentDueDate.plus(1, DateTimeUnit.MONTH)
            LocalDate(plusMonth.year, plusMonth.month, 6)
        }

        return resultDate.toString()
    }

    fun getDaysRemaining(platform: Platform, dueDateStr: String): Long {
        return try {
            val dueDate = LocalDate.parse(dueDateStr)
            val now = LocalDate.parse(platform.getCurrentDate())
            (dueDate.toEpochDays() - now.toEpochDays()).toLong()
        } catch (e: Exception) {
            -1
        }
    }

    fun isExpired(platform: Platform, dueDateStr: String): Boolean {
        return getDaysRemaining(platform, dueDateStr) < 0
    }

    fun shouldShowReminder(platform: Platform, dueDateStr: String): Boolean {
        val days = getDaysRemaining(platform, dueDateStr)
        return days in 0..3
    }
}
