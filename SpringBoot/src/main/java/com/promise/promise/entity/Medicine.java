package com.promise.promise.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "medicines")
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "primary_code", nullable = false, unique = true, length = 13)
    private String primaryCode;

    @Column(name = "standard_code", nullable = false, unique = true, length = 13)
    private String standardCode;

    @Column(name = "product_code", unique = true, length = 9)
    private String productCode;

    @Column(name = "category", length = 20)
    private String category;

    @Column(name = "manufacture", length = 20)
    private String manufacture;

    //Medicine 입장에서 Notification은 1:N 관계
    //하나의 약물은 여러 알림에 대응된다.
    @OneToMany(mappedBy = "medicine", fetch = FetchType.LAZY) //연관관계 주인이 아님, N 관계, 지연로딩
    private List<Notification> notifications = new ArrayList<>();

    //Medicine 입장에서 DUR은 1:N
    //하나의 약물은 여러 DUR에 대응된다.
    @OneToMany(mappedBy = "aMedicine", fetch = FetchType.LAZY)
    private List<DUR> DURAsAMedicine = new ArrayList<>();

    //Medicine 입장에서 DUR은 1:N
    //하나의 약물은 여러 DUR에 대응된다.
    @OneToMany(mappedBy = "bMedicine", fetch = FetchType.LAZY)
    private List<DUR> DURAsBMedicine = new ArrayList<>();
}
