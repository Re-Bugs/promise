package com.onlypromise.promise.controller.web.ocr;

import com.onlypromise.promise.DTO.MedicationDTO;
import com.onlypromise.promise.DTO.api.MedicineDTO;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.OcrProcessor;
import com.onlypromise.promise.service.VisionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("ocr")
@RequiredArgsConstructor
public class OcrController {

    private final VisionService visionService;
    private final OcrProcessor ocrProcessor;

    @GetMapping("/upload")
    public String showUploadForm() {
        return "ocr/upload";
    }

    @PostMapping("/extract-text")
    public String extractTextFromImage(MultipartFile file, RedirectAttributes redirectAttributes, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect://login";

        try
        {
            String extractedText = visionService.extractTextFromImage(file);
            List<MedicationDTO> dtoList = ocrProcessor.buildMedicationDtoList(extractedText);
            List<MedicineDTO> validDtoList = ocrProcessor.filterAndBuildValidDtoList(dtoList, user);
            List<String> warningMessages = ocrProcessor.saveNotifications(user, validDtoList);

            // 경고 메시지를 리스트로 설정
            if (!warningMessages.isEmpty())
            {
                redirectAttributes.addFlashAttribute("warningMessage", warningMessages);
            }

            redirectAttributes.addFlashAttribute("message", "처방전 인식이 성공적으로 완료되었습니다.");
        }
        catch (Exception e)
        {
            log.error("OCR error = {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "오류가 발생했습니다. 처방전 사진을 다시 찍어주세요.");
        }
        return "redirect:/admin/";
    }
}