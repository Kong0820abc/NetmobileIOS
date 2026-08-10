package com.xyz.netmobile

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object SubscriptionManager {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * 计算续费后的到期日期：
     * 如果已过期或新用户，续费至下个月 6 号。
     * 如果未过期，在原到期日基础上增加一个月，并对齐至 6 号。
     */
    fun calculateNextDueDate(currentDueDateStr: String?): String {
        val calendar = Calendar.getInstance()
        val now = calendar.time

        val currentDueDate = try {
            if (!currentDueDateStr.isNullOrEmpty()) dateFormat.parse(currentDueDateStr) else null
        } catch (e: Exception) {
            null
        }

        if (currentDueDate == null || currentDueDate.before(now)) {
            // 已过期或新用户：设为下个月 6 号
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 6)
        } else {
            // 未过期：在原到期日基础上加 1 个月，并对齐到 6 号
            calendar.time = currentDueDate
            calendar.add(Calendar.MONTH, 1)
            calendar.set(Calendar.DAY_OF_MONTH, 6)
        }

        return dateFormat.format(calendar.time)
    }

    /**
     * 获取剩余天数
     */
    fun getDaysRemaining(dueDateStr: String): Long {
        return try {
            val dueDate = dateFormat.parse(dueDateStr) ?: return -1
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val diff = dueDate.time - now.time
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 判定是否过期
     */
    fun isExpired(dueDateStr: String): Boolean {
        return getDaysRemaining(dueDateStr) < 0
    }

    /**
     * 是否需要显示温馨提示（到期前 1-3 天）
     */
    fun shouldShowReminder(dueDateStr: String): Boolean {
        val days = getDaysRemaining(dueDateStr)
        return days in 0..3
    }
}
