package com.onlypromise.promise.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import com.onlypromise.promise.R
import com.onlypromise.promise.MainActivity
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    private var ringtone: Ringtone? = null

    override fun onReceive(context: Context, intent: Intent) {
        // 알람 유형 (아침, 점심, 저녁)
        val alarmType = intent.getStringExtra("alarmType") ?: "알림"

        // 알람이 설정된 시간을 Intent로부터 받아옴
        val alarmTime = intent.getStringExtra("alarmTime") ?: "설정된 시간 없음"

        // 알람이 울린 현재 시간을 가져옴
        val currentTime = Calendar.getInstance().time
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = timeFormat.format(currentTime)

        // 알림 권한이 있는지 확인 (Android 13 이상)
        if (!checkNotificationPermission(context)) {
            Toast.makeText(context, "알림 권한이 없습니다. 설정에서 권한을 허용해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 알림을 클릭했을 때 열릴 액티비티 설정
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 알림 채널 생성 (Android 8.0 이상 필요)
        createNotificationChannel(context)

        // 알림 빌더 설정 (알림 제목, 내용에 설정된 시간과 현재 시간을 표시)
        val notificationBuilder = NotificationCompat.Builder(context, "alarm_channel_id")
            .setSmallIcon(R.drawable.ic_launcher_foreground)  // 기본 아이콘 설정
            .setContentTitle("약 먹을 시간입니다!")
            .setContentText("약을 잊지 말고 꼭 복용하세요!")  // 설정된 시간 및 알람 유형 표시
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // 높은 우선순위 설정
            .setAutoCancel(true)  // 알림을 클릭하면 자동으로 사라지도록 설정
            .setContentIntent(pendingIntent)

        // 알림 표시
        with(NotificationManagerCompat.from(context)) {
            notify(1001, notificationBuilder.build())
        }

        // 알람 소리 울리기 (무음 모드와 매너 모드에서도 강제 소리 재생)
        playAlarmSound(context)

        Log.d("AlarmReceiver", "알람이 울렸습니다: $alarmType, 설정된 알람 시간: $alarmTime")
    }

    // 알림 권한 확인 함수
    private fun checkNotificationPermission(context: Context): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 12 이하에서는 권한이 필요하지 않음
        }

        Log.d("AlarmReceiver", "Notification Permission: $hasPermission")
        return hasPermission
    }

    // 알람 소리를 강제 재생하는 함수 (무음 모드와 매너 모드에서도)
    private fun playAlarmSound(context: Context) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(context, alarmUri)

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 무음 모드 또는 매너 모드일 때 알람 소리 강제 재생
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT || audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            // 알람 스트림 볼륨을 최대치로 설정
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                AudioManager.FLAG_PLAY_SOUND
            )
        }

        ringtone?.play()
    }

    // 알림 채널 생성 함수 (Android 8.0 이상에서 필요)
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarm Notifications"
            val descriptionText = "알람을 위한 알림 채널"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("alarm_channel_id", name, importance).apply {
                description = descriptionText
            }

            // 시스템에 채널 등록
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}