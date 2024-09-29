package com.onlypromise.promise.repository;

import com.onlypromise.promise.domain.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {

    // 약품 코드로 Medicine 엔티티를 찾는 메서드
    Optional<Medicine> findByProductCode(String productCode);

    // 약품의 ID로 Medicine 엔티티를 찾는 메서드
    Optional<Medicine> findById(Long id);

    // 약품 코드로 Medicine 엔티티를 찾는 메서드 (부분 검색)
    @Query("SELECT m FROM Medicine m WHERE LOWER(m.productCode) LIKE LOWER(CONCAT('%', :productCode, '%'))")
    Optional<List<Medicine>> findByProductCodeContaining(@Param("productCode") String productCode);

    // 약품 이름으로 Medicine 엔티티를 찾는 메서드 (부분 검색)
    @Query("SELECT m FROM Medicine m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Optional<List<Medicine>> findByNameContaining(@Param("name") String name);
}
