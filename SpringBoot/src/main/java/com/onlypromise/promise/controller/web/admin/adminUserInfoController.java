package com.onlypromise.promise.controller.web.admin;

import com.onlypromise.promise.DTO.MedicationDTO;
import com.onlypromise.promise.DTO.MedicineDTO;
import com.onlypromise.promise.DTO.api.DailyTakenDTO;
import com.onlypromise.promise.domain.Medicine;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class adminUserInfoController {
    private final UserService userService;
    private final NotificationService notificationService;
    private final MedicationLogService medicationLogService;
    private final MedicineService medicineService;
    private final OcrProcessor ocrProcessor;
    private final VisionService visionService;

    @GetMapping("/{id}")
    public String getUserInfo(@PathVariable("id") Long id, @RequestParam(value = "date", required = false) String date, Model model) {
        Optional<User> findUser = userService.findUserById(id);
        if (findUser.isEmpty()) {
            return "redirect:/admin/home";
        }

        User user = findUser.get();
        List<Notification> notifications = notificationService.findNotificationByUser(user);

        // 날짜를 쿼리 파라미터로 받음, 없으면 오늘 날짜로 설정
        LocalDate targetDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();

        // 이전날, 다음날을 계산
        LocalDate previousDate = targetDate.minusDays(1);
        LocalDate nextDate = targetDate.plusDays(1);

        // 날짜별로 유저의 복용 상태 조회
        Map<String, List<DailyTakenDTO>> medicationStatus = notificationService.getMedicationsStatusByDate(user, targetDate);

        model.addAttribute("user", user);
        model.addAttribute("notifications", notifications);
        model.addAttribute("medicationStatus", medicationStatus);
        model.addAttribute("targetDate", targetDate); // 현재 날짜
        model.addAttribute("previousDate", previousDate); // 이전 날짜
        model.addAttribute("nextDate", nextDate); // 다음 날짜

        return "adminView/userInfo";
    }

    @PostMapping("/time_update")
    public String timeUpdate(
            @RequestParam String morningTime,
            @RequestParam String afternoonTime,
            @RequestParam String eveningTime,
            @RequestParam Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session)
    {
        User user = (User) session.getAttribute("user");
        if(user == null) return "redirect:/login";

        if(id != user.getId())
        {
            redirectAttributes.addFlashAttribute("errorMessage", "다른 관리자의 계정에 알림 시각을 변경 할 수 없습니다.");
            return "redirect:/admin/" + id;
        }

        // 입력된 시간을 LocalTime으로 변환하여 User 객체에 설정
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        User updateUser = user.toBuilder().morningTime(LocalTime.parse(morningTime, timeFormatter))
                .afternoonTime(LocalTime.parse(afternoonTime, timeFormatter))
                .eveningTime(LocalTime.parse(eveningTime, timeFormatter))
                .build();

        // 변경된 User 객체를 데이터베이스에 저장
        userService.save(updateUser);

        redirectAttributes.addFlashAttribute("message", "알림 시간이 변경되었습니다.");
        // 수정이 완료되면 원래 페이지로 리다이렉트
        return "redirect:/admin/" + id; // 수정 후 페이지를 리다이렉트
    }

    // 수동 약품 추가 검색 페이지로 이동하는 GET 메서드
    @GetMapping("/add_medicine")
    public String getSearchMedicine(Model model, @RequestParam Long id, RedirectAttributes redirectAttributes, HttpSession session)
    {
        User user = (User)session.getAttribute("user");

        if (user != null)
        {
            if(user.getId() != id)
            {
                redirectAttributes.addFlashAttribute("errorMessage", "다른 관리자의 계정에 수동 약물 추가를 할 수 없습니다.");
                return "redirect:/admin/" + id;
            }

            model.addAttribute("user", user);
            model.addAttribute("bottleId", user.getBottleId());
        }
        else
        {
            redirectAttributes.addFlashAttribute("errorMessage", "사용자를 찾을 수 없습니다.");
            return "redirect:/admin/info/" + id;
        }

        // 약품 추가 페이지로 이동
        return "adminView/addMedicine";
    }

    // 약품 추가 폼에서 약품을 저장하는 POST 메서드
    @PostMapping("/add_medicine")
    public String addMedicine(@RequestParam Long medicineId,
                              @RequestParam short totalDays,
                              @RequestParam(required = false) boolean morning,
                              @RequestParam(required = false) boolean afternoon,
                              @RequestParam(required = false) boolean evening,
                              RedirectAttributes redirectAttributes,
                              HttpSession session)
    {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";

        Optional<Medicine> findMedicine = medicineService.findMedicineById(medicineId);
        if (findMedicine.isEmpty())
        {
            redirectAttributes.addFlashAttribute("errorMessage", "약물을 찾을 수 없습니다.");
            return "redirect:/admin/" + user.getId();
        }

        Medicine medicine = findMedicine.get();

        // 알림 생성
        notificationService.createNotification(user, medicine.getId(), totalDays, morning, afternoon, evening);
        redirectAttributes.addFlashAttribute("message", "약물이 성공적으로 추가되었습니다.");

        return "redirect:/admin/" + user.getId();
    }

    // 약품 검색 요청을 처리하는 메서드
    @GetMapping("/search_medicine")
    public String searchMedicine( @RequestParam String identifier, Model model, HttpSession session)
    {
        User user = (User) session.getAttribute("user");
        if (user != null)
        {
            model.addAttribute("user", user);

            // 약품 이름 또는 코드로 약품을 검색
            Optional<List<MedicineDTO>> medicinesOptional = medicineService.findMedicineByNameOrProductCode(identifier);
            if (medicinesOptional.isPresent() && !medicinesOptional.get().isEmpty()) model.addAttribute("medicines", medicinesOptional.get()); // 검색된 약품 리스트 추가
            else model.addAttribute("error", "해당 약품을 찾을 수 없습니다.");
        }
        else return "redirect:/login";

        return "adminView/addMedicine"; // 검색 결과를 보여주는 페이지로 이동
    }

    @GetMapping("/upload")
    public String showAdminUploadForm(@RequestParam long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if(user == null) return "redirect:/login";
        if(id != user.getId())
        {
            redirectAttributes.addFlashAttribute("errorMessage", "다른 관리자의 계정에 처방전 인식을 할 수 없습니다.");
            return "redirect:/admin/" + id;
        }
        return "adminView/imageUpload";
    }

    @PostMapping("/extract-text")
    public String extractTextFromImage(MultipartFile file, RedirectAttributes redirectAttributes, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect://login";

        try
        {
            String extractedText = visionService.extractTextFromImage(file);
            List<MedicationDTO> dtoList = ocrProcessor.buildMedicationDtoList(extractedText);
            List<com.onlypromise.promise.DTO.api.MedicineDTO> validDtoList = ocrProcessor.filterAndBuildValidDtoList(dtoList, user);
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
        return "redirect:/admin/" + user.getId();
    }

    @PostMapping("/dosage")
    public String dosage(@RequestParam long id, RedirectAttributes redirectAttributes, HttpSession session)
    {
        User user = (User) session.getAttribute("user");

        if(user.getId() != id)
        {
            redirectAttributes.addFlashAttribute("errorMessage", "다른 관리자 계정에 약물 복용 기능을 사용할 수 없습니다.");
            return "redirect:/admin/" + id;
        }

        int status = medicationLogService.updateMedicationStatus(user.getBottleId());
        String message;

        switch(status)
        {
            case 0:
                message = "약물 복용을 기록하였습니다.";
                break;
            case 1:
                message = "계정의 약통코드를 찾을 수 없습니다.";
                break;
            case 2:
                message = "현재 시간대에 이미 복용 기록이 있습니다.";
                break;
            case 3:
                message = "약품이 모두 소진되어 복용기록을 남길 수 없습니다.";
                break;
            case 4:
                message = "현재 시간에 복용해야 할 약품이 없습니다.";
                break;
            default:
                message = "서버에 오류가 발생했습니다.";
        }

        redirectAttributes.addFlashAttribute(status == 0 ? "message" : "errorMessage", message);
        return "redirect:/admin/" + id;
    }
}
