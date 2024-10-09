package com.onlypromise.promise.domain;

import com.onlypromise.promise.domain.enumeration.NotificationValue;
import com.onlypromise.promise.domain.enumeration.Role;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true) // 객체 수정 허용
@Slf4j
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "user_id", unique = true, length = 20)
    private String userId;

    @Column(name = "user_password", length = 30)
    private String userPassword;

    @Column(name = "name", nullable = false, length = 5)
    private String name;

    private Byte age;

    @Column(name = "nick_name", unique = true, length = 10)
    private String nickName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_value", nullable = false)
    private NotificationValue notificationValue;

    @Column(name = "bottle_id", unique = true, length = 5)
    private String bottleId;

    @Column(length = 5)
    private String zipcode;

    // 시간 필드 추가
    @Column(name = "morning_time", nullable = false)
    @Builder.Default
    private LocalTime morningTime = LocalTime.of(8, 0);

    @Column(name = "afternoon_time", nullable = false)
    @Builder.Default
    private LocalTime afternoonTime = LocalTime.of(13, 0);

    @Column(name = "evening_time", nullable = false)
    @Builder.Default
    private LocalTime eveningTime = LocalTime.of(18, 0);

    // User와 Notification은 1:N 관계
    // 한 명의 사용자는 여러 알림을 받을 수 있다.
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY) // 연관관계 주인이 아님, 지연로딩
    private List<Notification> notifications;

    @PostLoad
    public void logInitialValues() {
        log.info("morning : {}", this.morningTime);
        log.info("afternoon : {}", this.afternoonTime);
        log.info("evening : {}", this.eveningTime);
    }
}