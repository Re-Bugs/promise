package com.onlypromise.promise.DTO.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReportDTO {

    @NotBlank(message = "약통코드는 필수 입니다.")
    private String bottleId;

    @NotBlank(message = "제목은 필수 입니다.")
    @Size(min = 2, max = 20, message = "제목은 2 ~ 20자 사이여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수 입니다.")
    @Size(min = 5, message = "내용을 최소 5글자 입력해주세요.")
    private String content;
}
