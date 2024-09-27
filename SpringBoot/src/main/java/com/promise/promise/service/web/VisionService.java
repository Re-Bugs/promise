package com.promise.promise.service.web;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.promise.promise.domain.Medicine;
import com.promise.promise.repository.web.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VisionService {

    private final MedicineRepository medicineRepository;

    public String extractTextFromImage(MultipartFile image) throws Exception {
        return extractText(ByteString.readFrom(image.getInputStream()));
    }

    private String extractText(ByteString imgBytes) throws Exception {
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();
        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            StringBuilder stringBuilder = new StringBuilder();

            for (AnnotateImageResponse res : response.getResponsesList()) {
                if (res.hasError()) {
                    return "Error: " + res.getError().getMessage();
                }
                stringBuilder.append(res.getFullTextAnnotation().getText());
            }
            return stringBuilder.toString();
        }
    }

    public Optional<Medicine> getMedicineByProductCode(String productCode) {
        return medicineRepository.findByProductCode(productCode);
    }

    // ID로 Medicine 객체를 조회하는 메서드
    public Optional<Medicine> getMedicineById(Long id) {
        return medicineRepository.findById(id);
    }
}