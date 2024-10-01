package com.onlypromise.promise.DTO.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {
    @NotBlank(message = "이름 필수 항목입니다.")
    private String name;

    @NotBlank(message = "약통코드는 필수 항목입니다.")
    private String bottleId;

    @NotNull
    private Byte age;
}
