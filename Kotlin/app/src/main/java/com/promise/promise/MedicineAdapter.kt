// 파일명: MedicineAdapter.kt
package com.promise.promise

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 복약 여부를 나타내는 어댑터 클래스
class MedicineAdapter(private val medicineList: List<MedicineItem>) :
    RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val medicineNameTextView: TextView = itemView.findViewById(R.id.medicineNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.medicine_item, parent, false)
        return MedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        val medicine = medicineList[position]
        holder.medicineNameTextView.text = medicine.medicineName

        // 복약 여부에 따라 색상 변경 (복용 시 초록색, 미복용 시 빨간색)
        holder.medicineNameTextView.setTextColor(
            if (medicine.isTaken) Color.GREEN else Color.RED
        )
    }

    override fun getItemCount(): Int = medicineList.size
}