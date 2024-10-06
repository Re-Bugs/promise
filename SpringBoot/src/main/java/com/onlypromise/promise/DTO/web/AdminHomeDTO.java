package com.onlypromise.promise.DTO.web;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminHomeDTO {
    private long id; //user PK

    private String name;

    private Byte age;

    private String bottleId;

    private int totalMedicine;

    private double percent;
}
