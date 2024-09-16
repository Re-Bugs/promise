package com.promise.promise.web.controller.ocr;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MedicationDTO {
    private String medicationCode;
    private String totalDosageDays;
    private String dailyDosageTimes;
    private List<String> mealTimes;
}