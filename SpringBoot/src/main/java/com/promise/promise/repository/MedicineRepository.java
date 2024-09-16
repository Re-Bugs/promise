package com.promise.promise.repository;

import com.promise.promise.domain.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    Optional<Medicine> findByProductCode(String productCode);
}
