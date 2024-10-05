package com.onlypromise.promise

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 데이터 클래스
data class NotificationItem(
    val id: Int,
    val name: String,
    val remainingDose: Int,
    val renewalDate: String,
    val doseTimes: String
)

// RecyclerView Adapter
class NotificationAdapter(private val notifications: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    // ViewHolder 정의
    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.notificationName)
        val remainingDoseTextView: TextView = itemView.findViewById(R.id.remainingDose)
        val renewalDateTextView: TextView = itemView.findViewById(R.id.renewalDate)
        val doseTimesTextView: TextView = itemView.findViewById(R.id.doseTimes)
    }

    // ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.notification_item, parent, false)
        return NotificationViewHolder(view)
    }

    // ViewHolder에 데이터 바인딩
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.nameTextView.text = notification.name
        holder.remainingDoseTextView.text = "남은 복용량: ${notification.remainingDose}"
        holder.renewalDateTextView.text = "갱신 날짜: ${notification.renewalDate}"
        holder.doseTimesTextView.text = "복용 시간: ${notification.doseTimes}"
    }

    // 아이템 개수 반환
    override fun getItemCount() = notifications.size
}