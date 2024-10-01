package com.onlypromise.promise.controller.web;

import com.onlypromise.promise.DTO.DailyTakenDTO;
import com.onlypromise.promise.DTO.MedicineDTO;
import com.onlypromise.promise.DTO.api.LoginDTO;
import com.onlypromise.promise.domain.Medicine;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.MedicineService;
import com.onlypromise.promise.service.NotificationService;
import com.onlypromise.promise.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final MedicineService medicineService;

    @GetMapping
    public String testHome(LoginDTO loginDTO, Model model)
    {
        LocalDateTime currentTime = LocalDateTime.now();
        String formattedDateTime = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        model.addAttribute(loginDTO);
        model.addAttribute("currentDateTime", formattedDateTime); // 문자열로 변환된 시간을 넘김
        return "test/test";
    }


    @PostMapping("/login")
    public String loginForm(LoginDTO loginDTO, RedirectAttributes redirectAttributes)
    {
        Optional<User> findUser = userService.findUserByBottleId(loginDTO.getBottleId());
        if (findUser.isPresent())
        {
            User user = findUser.get();
            User updateUser = user.toBuilder().name(loginDTO.getName()).age(loginDTO.getAge()).build();
            userService.save(updateUser);
            return "redirect:/test/info/" + updateUser.getId();  // 유저 PK를 기반으로 리다이렉트
        }
        else
        {
            redirectAttributes.addFlashAttribute("loginErrorMessage", "약통 코드를 확인해주세요.");
            return "redirect:/test";
        }
    }

    // /info/{id} 경로로 유저 정보 조회
    @GetMapping("/info/{id}")
    public String testInfo(@PathVariable("id") Long id, Model model)
    {
        // 유저 정보 및 알림 정보 조회
        Optional<User> userOptional = userService.findUserById(id);  // PK 기반으로 조회

        if (userOptional.isPresent())
        {
            User user = userOptional.get();
            List<Notification> notifications = userService.findNotificationsByUser(user);  // 사용자 기반으로 알림 조회

            LocalDate date = LocalDate.now();

            // 날짜별로 유저의 복용 상태를 확인
            Map<String, List<DailyTakenDTO>> medicationStatus = notificationService.getMedicationsStatusByDate(user, date);
            model.addAttribute("user", user);  // 사용자 정보 전달
            model.addAttribute("notifications", notifications);  // 알림 정보 전달
            model.addAttribute("medicationStatus", medicationStatus);  // 복용 정보 전달
            log.info("log = {}", medicationStatus);

        }
        else model.addAttribute("errorMessage", "사용자를 찾을 수 없습니다.");

        return "test/info";  // info 페이지로 이동
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
        return "redirect:/test/info/" + id; // 수정 후 페이지를 리다이렉트
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
        return "test/addMedicine";
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
            return "redirect:/test/info/" + id;
        }

        return "test/addMedicine"; // 검색 결과를 보여주는 페이지로 이동
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
            return "redirect:/test/info/" + id;
        }

        // 약물 추가 로직
        Optional<User> findUser = userService.findUserById(id);
        if (findUser.isEmpty())
        {
            redirectAttributes.addFlashAttribute("error", "유저 정보를 찾을 수 없습니다.");
            return "redirect:/test/info/" + id;
        }

        Optional<Medicine> findMedicine = medicineService.findMedicineById(medicineId);
        if (findMedicine.isEmpty())
        {
            redirectAttributes.addFlashAttribute("error", "약물을 찾을 수 없습니다.");
            return "redirect:/test/info/" + id;
        }

        Medicine medicine = findMedicine.get();

        // 알림 생성
        notificationService.createNotification(bottleId, medicine.getId(), totalDays, morning, afternoon, evening);
        redirectAttributes.addFlashAttribute("message", "약물이 성공적으로 추가되었습니다.");

        return "redirect:/test/info/" + id;
    }
}