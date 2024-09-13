package com.promise.promise.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "medicines")
public class Medicines {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "primary_code", nullable = false, length = 13)
    private String primaryCode;

    @Column(name = "standard_code", nullable = false, length = 13)
    private String standardCode;

    @Column(name = "product_code", length = 9)
    private String productCode;

    @Column(name = "category", length = 20)
    private String category;

    @Column(name = "manufacture", length = 20)
    private String manufacture;
}
