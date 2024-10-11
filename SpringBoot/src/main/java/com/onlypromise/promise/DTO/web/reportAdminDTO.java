package com.onlypromise.promise.DTO.web;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class reportAdminDTO {
    private long id;
    private String userName;
    private String title;
    private String creatAt;
}
