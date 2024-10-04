package com.promise.promise.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import java.util.Calendar

object AlarmManagerUtils {

    // 알람 설정/취소 함수
    fun manageAlarm(
        context: Context,
        time: String,
        type: String,
        shouldSetAlarm: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (shouldSetAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            showExactAlarmPermissionDialog(context)
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmType", type)
            putExtra("alarmTime", time)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (shouldSetAlarm) {
                val timeParts = time.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)

                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                Log.d("AlarmManager", "$type 알람이 설정되었습니다.")

                // 알람 시간을 SharedPreferences에 저장
                saveAlarmTime(context, time, type)
            } else {
                alarmManager.cancel(pendingIntent)
                Log.d("AlarmManager", "$type 알람이 취소되었습니다.")
                removeAlarmTime(context, type)
            }
        } catch (e: SecurityException) {
            showExactAlarmPermissionDialog(context)
        }
    }

    private fun saveAlarmTime(context: Context, time: String, type: String) {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(type, time)
            apply()
        }
    }

    private fun removeAlarmTime(context: Context, type: String) {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove(type)
            apply()
        }
    }

    // 저장된 알람을 복원하는 함수
    fun restoreAlarms(context: Context) {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        val morningTime = sharedPref.getString("morning", null)
        val afternoonTime = sharedPref.getString("afternoon", null)
        val eveningTime = sharedPref.getString("evening", null)

        if (morningTime != null) {
            manageAlarm(context, morningTime, "morning", true)
        }
        if (afternoonTime != null) {
            manageAlarm(context, afternoonTime, "afternoon", true)
        }
        if (eveningTime != null) {
            manageAlarm(context, eveningTime, "evening", true)
        }
    }

    private fun showExactAlarmPermissionDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("정확한 알람 설정 권한 필요")
            .setMessage("정확한 알람을 설정하려면 권한이 필요합니다. 설정으로 이동하여 권한을 활성화해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
