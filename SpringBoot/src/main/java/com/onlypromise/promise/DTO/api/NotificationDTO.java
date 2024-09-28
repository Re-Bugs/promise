package com.onlypromise.promise.DTO.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private long id;
    private short remainingDose;
    private LocalDate renewalDate;
    private String dailyDose;
    private boolean morning;
    private boolean afternoon;
    private boolean evening;
}
