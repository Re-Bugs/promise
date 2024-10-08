package com.onlypromise.promise.DTO.web;

import com.onlypromise.promise.domain.enumeration.Role;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminHomeDTO {
    private long id; //user PK

    private Role role;

    private String name;

    private Byte age;

    private String bottleId;

    private int logSize;

    private int totalMedicine;

    private double percent;
}
