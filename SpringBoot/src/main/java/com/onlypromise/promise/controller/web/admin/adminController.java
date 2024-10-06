package com.onlypromise.promise.controller.web.admin;

import com.onlypromise.promise.DTO.MedicineDTO;
import com.onlypromise.promise.DTO.api.DailyTakenDTO;
import com.onlypromise.promise.DTO.web.AdminHomeDTO;
import com.onlypromise.promise.domain.MedicationLog;
import com.onlypromise.promise.domain.Medicine;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.repository.MedicationLogRepository;
import com.onlypromise.promise.service.MedicineService;
import com.onlypromise.promise.service.NotificationService;
import com.onlypromise.promise.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class adminController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final MedicationLogRepository medicationLogRepository;
    private final MedicineService medicineService;


    @GetMapping("/home")
    public String adminHome(HttpSession session, Model model)
    {
        User user = (User) session.getAttribute("user");
        if(user == null) return "redirect:/login";

        // 2024년 9월 30일 00:00부터 오늘까지의 범위 지정
        LocalDateTime startDateTime = LocalDateTime.of(2024, 9, 30, 0, 0);
        LocalDateTime endDateTime = LocalDateTime.now(); // 현재 시간까지

        // LocalDate로 변환하여 두 날짜의 차이를 구함
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDateTime.toLocalDate(), endDateTime.toLocalDate());
        log.info("두 날짜의 차이 : {}", daysBetween);

        List<AdminHomeDTO> dtoList = new ArrayList<>();
        double totalPercent = 0.0;
        DecimalFormat df = new DecimalFormat("#.##"); // 소수점 두 자리까지만 표현
        for(User u : userService.findAllUser())
        {
            AdminHomeDTO newDto = new AdminHomeDTO();
            newDto.setId(u.getId());
            newDto.setName(u.getName());
            newDto.setAge(u.getAge());
            newDto.setBottleId(u.getBottleId());
            int notificationSize = notificationService.findNotificationByUser(u).size();
            newDto.setTotalMedicine(notificationSize);

            if (daysBetween == 0 || notificationSize == 0) newDto.setPercent(0); // 알림이 없는 경우 0%로 설정
            else
            {
                int logs = medicationLogRepository.findByUserAndTimeBetween(u, startDateTime, endDateTime).size();
                double percent = (logs / (daysBetween * (double) notificationSize)) * 100;
                newDto.setPercent(Double.parseDouble(df.format(percent)));
                totalPercent += newDto.getPercent();
            }
            dtoList.add(newDto);
        }
        totalPercent = Double.parseDouble(df.format((totalPercent - dtoList.get(10).getPercent() - dtoList.get(11).getPercent()) / (dtoList.size() - 2)));
        model.addAttribute("userInfo", dtoList);
        model.addAttribute("totalPercent", totalPercent);
        return "adminView/home";
    }

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
            RedirectAttributes redirectAttributes
    ) {
        log.info("id = {}", id);
        // 유저를 데이터베이스에서 찾아옴
        Optional<User> findUser = userService.findUserById(id);

        // 입력된 시간을 LocalTime으로 변환하여 User 객체에 설정
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        User updateUser = findUser.get().toBuilder().morningTime(LocalTime.parse(morningTime, timeFormatter))
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
    public String getSearchMedicine(Model model, @RequestParam Long id)
    {
        // 현재 사용자의 유효성을 확인하고, 필요한 데이터를 가져옴
        Optional<User> userOptional = userService.findUserById(id);

        if (userOptional.isPresent())
        {
            User user = userOptional.get();
            model.addAttribute("user", user);
            model.addAttribute("bottleId", user.getBottleId());  // 사용자 보틀 ID 가져오기
        }
        else
        {
            model.addAttribute("error", "사용자를 찾을 수 없습니다.");
            return "redirect:/test/info/" + id;
        }

        // 약품 추가 페이지로 이동
        return "adminView/addMedicine";
    }

    // 약품 검색 요청을 처리하는 메서드
    @GetMapping("/search_medicine")
    public String searchMedicine(@RequestParam Long id, @RequestParam String identifier, Model model)
    {
        // 사용자를 찾아서 모델에 추가
        Optional<User> userOptional = userService.findUserById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
            model.addAttribute("bottleId", user.getBottleId());

            // 약품 이름 또는 코드로 약품을 검색
            Optional<List<MedicineDTO>> medicinesOptional = medicineService.findMedicineByNameOrProductCode(identifier);
            if (medicinesOptional.isPresent() && !medicinesOptional.get().isEmpty()) model.addAttribute("medicines", medicinesOptional.get()); // 검색된 약품 리스트 추가
            else model.addAttribute("error", "해당 약품을 찾을 수 없습니다.");
        }
        else
        {
            model.addAttribute("error", "사용자를 찾을 수 없습니다.");
            return "redirect:/admin/" + id;
        }

        return "adminView/addMedicine"; // 검색 결과를 보여주는 페이지로 이동
    }

    // 약품 추가 폼에서 약품을 저장하는 POST 메서드
    @PostMapping("/add_medicine")
    public String addMedicine(@RequestParam(required = false) String bottleId,
                              @RequestParam Long medicineId,
                              @RequestParam short totalDays,
                              @RequestParam(required = false) boolean morning,
                              @RequestParam(required = false) boolean afternoon,
                              @RequestParam(required = false) boolean evening,
                              @RequestParam Long id,
                              RedirectAttributes redirectAttributes)
    {
        // bottleId가 없을 경우 에러 처리
        if (bottleId == null || bottleId.isEmpty())
        {
            redirectAttributes.addFlashAttribute("error", "약통 ID를 찾을 수 없습니다.");
            return "redirect:/admin/" + id;
        }

        // 약물 추가 로직
        Optional<User> findUser = userService.findUserById(id);
        if (findUser.isEmpty())
        {
            redirectAttributes.addFlashAttribute("error", "유저 정보를 찾을 수 없습니다.");
            return "redirect:/admin/" + id;
        }

        Optional<Medicine> findMedicine = medicineService.findMedicineById(medicineId);
        if (findMedicine.isEmpty())
        {
            redirectAttributes.addFlashAttribute("error", "약물을 찾을 수 없습니다.");
            return "redirect:/admin/" + id;
        }

        Medicine medicine = findMedicine.get();

        // 알림 생성
        notificationService.createNotification(bottleId, medicine.getId(), totalDays, morning, afternoon, evening);
        redirectAttributes.addFlashAttribute("message", "약물이 성공적으로 추가되었습니다.");

        return "redirect:/admin/" + id;
    }
}
