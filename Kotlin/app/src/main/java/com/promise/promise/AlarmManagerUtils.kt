package com.promise.promise.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.promise.promise.AlarmReceiver
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

        // API 레벨 31 이상에서는 canScheduleExactAlarms() 확인
        if (shouldSetAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            showExactAlarmPermissionDialog(context)
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmType", type)
            putExtra("alarmTime", time)  // 알람 시간이 포함됨
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (shouldSetAlarm) {
                // 알람 설정 로직
                val timeParts = time.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)

                    // 현재 시간보다 이전 시간일 경우 다음날로 설정
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1)
                    }
                }

                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                Log.d("AlarmManager", "$type 알람이 설정되었습니다.")
            } else {
                // 알람 취소 로직
                alarmManager.cancel(pendingIntent)
                Log.d("AlarmManager", "$type 알람이 취소되었습니다.")
            }
        } catch (e: SecurityException) {
            // 권한이 없을 때 처리
            showExactAlarmPermissionDialog(context)
        }
    }

    // 권한 요청을 위한 다이얼로그 표시
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