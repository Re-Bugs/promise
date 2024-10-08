package com.onlypromise.promise.DTO.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class MedicineDTO {
    private long medicineId;
    private String totalDosageDays;
    private String dailyDosageTimes;
    private List<String> mealTimes;
}
