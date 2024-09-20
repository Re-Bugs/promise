package com.promise.promise.api.controller;

import com.promise.promise.api.DTO.SignUpDTO;
import com.promise.promise.api.service.UserAPIService;
import com.promise.promise.domain.MedicationLog;
import com.promise.promise.domain.Notification;
import com.promise.promise.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestAPIController {

    private final UserAPIService userAPIService;

    @GetMapping
    public String testHome(SignUpDTO signUpDTO, Model model) {
        LocalDateTime currentTime = LocalDateTime.now();
        String formattedDateTime = currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        model.addAttribute(signUpDTO);
        model.addAttribute("currentDateTime", formattedDateTime); // 문자열로 변환된 시간을 넘김
        return "test/test";
    }

    @PostMapping
    public String submitSignUpForm(@Valid SignUpDTO signUpDTO, RedirectAttributes redirectAttributes) {
        boolean isSignUpSuccessful = userAPIService.testSignUp(signUpDTO);

        if (isSignUpSuccessful) {
            // 성공 메시지와 함께 리다이렉트
            redirectAttributes.addFlashAttribute("message", "회원가입이 성공적으로 완료되었습니다.");
            return "redirect:/test"; // PRG 패턴으로 리다이렉트
        } else {
            redirectAttributes.addFlashAttribute("message", "중복된 bottle ID가 존재하거나 이름 또는 약통 코드를 확인해주세요.");
            return "redirect:/test"; // 다시 폼으로 리다이렉트
        }
    }

    @PostMapping("/login")
    public String loginForm(@RequestParam String bottleId, RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userAPIService.findUserByBottleId(bottleId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return "redirect:/test/info/" + user.getId();  // 유저 PK를 기반으로 리다이렉트
        } else {
            redirectAttributes.addFlashAttribute("loginErrorMessage", "약통 코드를 확인해주세요.");
            return "redirect:/test";
        }
    }

    // /info/{id} 경로로 유저 정보 조회
    @GetMapping("/info/{id}")
    public String testInfo(@PathVariable("id") Long id, Model model) {
        // 유저 정보 및 알림 정보 조회
        Optional<User> userOptional = userAPIService.findUserById(id);  // PK 기반으로 조회

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            List<Notification> notifications = userAPIService.findNotificationsByUser(user);  // 사용자 기반으로 알림 조회
            List<MedicationLog> medicationLogs = userAPIService.findMedicationLogsByUser(user);  // 사용자 기반으로 복용 기록 조회

            model.addAttribute("user", user);  // 사용자 정보 전달
            model.addAttribute("notifications", notifications);  // 알림 정보 전달
            model.addAttribute("medicationLogs", medicationLogs);  // 복용 기록 정보 전달
        } else {
            model.addAttribute("errorMessage", "사용자를 찾을 수 없습니다.");
        }

        return "test/info";  // info 페이지로 이동
    }
}