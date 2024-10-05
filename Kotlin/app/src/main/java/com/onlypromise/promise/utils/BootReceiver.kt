package com.onlypromise.promise.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "부팅 완료됨. 알람을 재설정합니다.")
            // 재부팅 후 저장된 알람을 재설정
            AlarmManagerUtils.restoreAlarms(context)
        }
    }
}
