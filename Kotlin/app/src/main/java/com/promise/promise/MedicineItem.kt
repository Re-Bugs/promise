package com.promise.promise

// 복약 여부를 나타내는 데이터 클래스
data class MedicineItem(
    val id: Int,
    val medicineName: String,
    val isTaken: Boolean
)