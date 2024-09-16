package com.promise.promise.service;

import com.promise.promise.domain.Medicine;
import com.promise.promise.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;

    // ID로 약품 정보를 가져오는 메서드
    public Medicine findById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 약품이 존재하지 않습니다: " + id));
    }

    // 약품 코드를 통해 약품을 가져오는 메서드
    public Optional<Medicine> getMedicineByProductCode(String productCode) {
        return medicineRepository.findByProductCode(productCode);
    }
}
