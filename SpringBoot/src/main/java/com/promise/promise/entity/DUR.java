package com.promise.promise.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "dur")
public class DUR {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // a_product_code와 b_product_code는 medicines 테이블의 product_code를 참조
    //DUR 입장에서 Medicine은 N:1 관계
    //여러 DUR은 하나의 약물에 대응된다.
    @ManyToOne(fetch = FetchType.LAZY) //연관관계 주인, 지연로딩
    @JoinColumn(name = "a_product_code", referencedColumnName = "product_code", nullable = false)
    private Medicine aMedicine;

    @ManyToOne(fetch = FetchType.LAZY) //연관관계 주인, 지연로딩
    @JoinColumn(name = "b_product_code", referencedColumnName = "product_code", nullable = false)
    private Medicine bMedicine;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(length = 100)
    private String etc;
}
