package com.promise.promise.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

@Entity
@Table(name = "users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "user_id", length = 20)
    private String userId;

    @Column(name = "user_password", length = 30)
    private String userPassword;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Range(min = 1, max = 130)
    private byte age;

    @Column(name = "nick_name", length = 10)
    private String nickName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_value")
    private NotificationValue notificationValue;

    @Column(name = "bottle_id")
    private String bottleId;

    private String zipcode;
}
