package com.onlypromise.promise.domain;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Getter
@Entity
@Table(name = "reports")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true) // 객체 수정 허용
@Slf4j
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    //Report 입장에서 User 와 관계는 N:1
    //여러 레포트는 하나의 사용자와 대응된다.
    @ManyToOne(fetch = FetchType.LAZY) //연관관계 주인, 지연로딩
    @JoinColumn(name = "user_absolute_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;
}
