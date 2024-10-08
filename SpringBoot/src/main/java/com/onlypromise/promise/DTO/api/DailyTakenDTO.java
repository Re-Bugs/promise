package com.onlypromise.promise.DTO.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DailyTakenDTO {
    private Long id;
    private String medicineName;
}
