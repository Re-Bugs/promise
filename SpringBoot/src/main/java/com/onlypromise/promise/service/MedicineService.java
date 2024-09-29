package com.onlypromise.promise.service;

import com.onlypromise.promise.domain.Medicine;
import com.onlypromise.promise.DTO.MedicineDTO;
import com.onlypromise.promise.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;

    public Optional<List<MedicineDTO>> findMedicineByNameOrProductCode(String identifier) {
        // 공백 제거 및 소문자로 변환
        String trimmedIdentifier = identifier.trim().toLowerCase();

        // 먼저 product_code로 검색
        Optional<List<Medicine>> medicineOptionalList = medicineRepository.findByProductCodeContaining(trimmedIdentifier);

        // product_code로 찾지 못했을 경우 name으로 검색
        if (medicineOptionalList.isEmpty() || medicineOptionalList.get().isEmpty()) {
            medicineOptionalList = medicineRepository.findByNameContaining(trimmedIdentifier);
        }

        // 약물이 존재할 경우 DTO로 변환하여 리턴
        if (medicineOptionalList.isPresent() && !medicineOptionalList.get().isEmpty()) {
            List<MedicineDTO> medicineDTOList = medicineOptionalList.get().stream()
                    .map(medicine -> new MedicineDTO(medicine.getId(), medicine.getName(), medicine.getCategory(), medicine.getManufacturer()))
                    .collect(Collectors.toList());
            return Optional.of(medicineDTOList);
        }

        return Optional.empty();
    }

    public Optional<Medicine> findMedicineById(Long id)
    {
        return medicineRepository.findById(id);
    }
}