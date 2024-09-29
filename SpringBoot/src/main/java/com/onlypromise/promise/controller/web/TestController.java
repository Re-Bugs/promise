package com.onlypromise.promise.controller.web;

import com.onlypromise.promise.DTO.DailyTakenDTO;
import com.onlypromise.promise.DTO.api.SignUpDTO;
import com.onlypromise.promise.domain.MedicationLog;
import com.onlypromise.promise.domain.Notification;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.service.NotificationService;
import com.onlypromise.promise.service.UserService;
import jakarta.validation.Valid;
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

    @GetMapping
    public String testHome(SignUpDTO signUpDTO, Model model)
    {
        LocalDateTime currentTime = LocalDateTime.now();
        String formattedDateTime = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        model.addAttribute(signUpDTO);
        model.addAttribute("currentDateTime", formattedDateTime); // 문자열로 변환된 시간을 넘김
        return "test/test";
    }

    @PostMapping
    public String submitSignUpForm(@Valid SignUpDTO signUpDTO, RedirectAttributes redirectAttributes)
    {
        boolean isSignUpSuccessful = userService.testSignUp(signUpDTO);

        if (isSignUpSuccessful)
        {
            redirectAttributes.addFlashAttribute("message", "회원가입이 성공적으로 완료되었습니다.");
            return "redirect:/test"; // PRG 패턴으로 리다이렉트
        }
        else
        {
            redirectAttributes.addFlashAttribute("errorMessage", "중복된 약통코드가 존재하거나 이름 또는 약통 코드를 확인해주세요.");
            return "redirect:/test"; // 다시 폼으로 리다이렉트
        }
    }

    @PostMapping("/login")
    public String loginForm(@RequestParam String bottleId, RedirectAttributes redirectAttributes)
    {
        Optional<User> userOptional = userService.findUserByBottleId(bottleId);
        if (userOptional.isPresent())
        {
            User user = userOptional.get();
            return "redirect:/test/info/" + user.getId();  // 유저 PK를 기반으로 리다이렉트
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
}