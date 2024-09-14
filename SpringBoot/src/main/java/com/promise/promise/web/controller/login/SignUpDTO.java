package com.promise.promise.web.controller.login;

import com.promise.promise.domain.enumeration.NotificationValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpDTO {

    @NotBlank(message = "사용자 ID는 필수 항목입니다.")
    private String userId;

    @NotBlank(message = "비밀번호는 필수 항목입니다.")
    private String userPassword;

    @NotBlank(message = "이름은 필수 항목입니다.")
    private String name;

    @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
    @Max(value = 130, message = "나이는 130 이하여야 합니다.")
    private byte age;

    @NotBlank(message = "닉네임은 필수 항목입니다.")
    private String nickName;

    @NotNull(message = "알림 설정은 필수 항목입니다.")
    private NotificationValue notificationValue;

    private String bottleId;
    private String zipcode;
}