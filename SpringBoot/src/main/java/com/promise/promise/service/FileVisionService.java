package com.promise.promise.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class FileVisionService {

    public String extractTextFromFile(MultipartFile file) throws Exception {
        // MultipartFile을 ByteString으로 변환
        ByteString imgBytes = ByteString.copyFrom(file.getBytes());

        // OCR 기능을 사용하여 이미지에서 텍스트 추출
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                .addFeatures(feat)
                .setImage(img)
                .build();
        List<AnnotateImageRequest> requests = new ArrayList<>();
        requests.add(request);

        // Google Cloud Vision API 클라이언트 생성 및 요청 실행
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            StringBuilder stringBuilder = new StringBuilder();

            // 응답 처리 및 텍스트 추출
            for (AnnotateImageResponse res : response.getResponsesList()) {
                if (res.hasError()) {
                    System.out.printf("Error: %s\n", res.getError().getMessage());
                    return "Error detected: " + res.getError().getMessage();
                }
                stringBuilder.append(res.getFullTextAnnotation().getText());
            }
            return stringBuilder.toString();
        }
    }
}