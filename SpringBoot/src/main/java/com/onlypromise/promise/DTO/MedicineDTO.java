package com.onlypromise.promise.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MedicineDTO {
    private Long id;
    private String name;
    private String category;
    private String manufacturer;
}