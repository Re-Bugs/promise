package com.promise.promise.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "medication_log")
public class MedicationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    //MedicationLog 입장에서 Notification은 N:1 관계
    //여러 복용기록은 하나의 알림에 대응된다.
    @ManyToOne(fetch = FetchType.LAZY) //연관관계 주인, 지연로딩
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    //MedicationLog 입장에서 User는 N:1 관계
    //여러 복용 기록은 하나의 사용자에 대응된다.
    @ManyToOne(fetch = FetchType.LAZY) //연관관계 주인, 지연로딩
    @JoinColumn(name = "user_absolute_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime time;

    @Column(nullable = false)
    private Boolean status;

    //연관관계 편의 메서드(복용기록에서 알림추가)
    public void addNotification(Notification notification)
    {
        this.notification = notification;
        notification.getMedicationLogs().add(this);
    }
}
