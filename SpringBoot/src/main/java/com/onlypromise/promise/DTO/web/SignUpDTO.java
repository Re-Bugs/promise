package com.onlypromise.promise.DTO.web;

import com.onlypromise.promise.domain.enumeration.NotificationValue;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.common.aliasing.qual.Unique;

@Getter @Setter
public class SignUpDTO {

    @Size(min = 3, message = "아이디가 너무 짧습니다.")
    @NotBlank(message = "아이디는 필수 항목입니다.")
    private String userId;

    @Size(min = 8, message = "비밀번호가 너무 짧습니다.")
    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String userPassword;

    @Size(min = 2, max = 5, message = "이름의 길이가 잘못되었습니다.")
    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @Min(value = 14, message = "14세 이상 회원가입 가능합니다.")
    @Max(value = 130, message = "잘못된 값을 입력했습니다.")
    private byte age;

    @NotBlank(message = "닉네임은 필수 항목입니다.")
    @Unique
    @Size(min = 2, max = 10, message = "닉네임의 길이는 2 ~ 10 글자입니다.")
    private String nickName;

    @NotNull(message = "알림 설정은 필수 항목입니다.")
    private NotificationValue notificationValue;

    private String bottleId;

    private String zipcode;
}