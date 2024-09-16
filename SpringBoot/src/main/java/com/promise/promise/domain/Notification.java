package com.promise.promise.domain;

import com.promise.promise.domain.enumeration.DailyDose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "notifications")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    //Notification 입장에서 Medicine 과의 관계는 N:1
    //여러 알림은 하나의 약과 대응된다.(다수의 사용자가 하나의 약을 복용할 경우)
    @ManyToOne(fetch = FetchType.LAZY) //연관관계 주인, 지연로딩
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    //Notification 입장에서 User 와 관계는 N:1
    //여러 알림은 하나의 사용자와 대응된다.
    @ManyToOne(fetch = FetchType.LAZY) //연관관계 주인, 지연로딩
    @JoinColumn(name = "user_absolute_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    private short total;

    @Column(name = "remaining_dose")
    private short remainingDose;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "daily_dose", nullable = false)
    private DailyDose dailyDose;

    private Boolean morning;

    private Boolean afternoon;

    private Boolean evening;

    //Notification 입장에서 MedicineLog와 1:N 관계
    //하나의 알림(약물)은 여러 복용기록을 가질 수 있다.
    @OneToMany(mappedBy = "notification", fetch = FetchType.LAZY) //연관관계 주인이 아님, 지연로딩
    private List<MedicationLog> medicationLogs = new ArrayList<>();

    //연관관계 편의 메서드(알림에서 약물 추가)
    public void addMedicine(Medicine medicine)
    {
        this.medicine = medicine;
        medicine.getNotifications().add(this);
    }

    //연관관계 편의 메서드(알림에서 사용자 추가)
    public void addUser(User user)
    {
        this.user = user;
        user.getNotifications().add(this);
    }
}
