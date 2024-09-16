package com.promise.promise.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class VisionService {

    public String extractTextFromImage(MultipartFile image) throws Exception {
        return extractText(ByteString.readFrom(image.getInputStream()));
    }

    public String extractTextFromFile(MultipartFile file) throws Exception {
        return extractText(ByteString.copyFrom(file.getBytes()));
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
}