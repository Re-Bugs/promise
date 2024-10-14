package com.onlypromise.promise.controller.web.admin;

import com.onlypromise.promise.DTO.web.AdminHomeDTO;
import com.onlypromise.promise.DTO.web.reportAdminDTO;
import com.onlypromise.promise.domain.Report;
import com.onlypromise.promise.domain.User;
import com.onlypromise.promise.domain.enumeration.Role;
import com.onlypromise.promise.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class adminHomeController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final MedicationLogService medicationLogService;
    private final ReportService reportService;

    private final DateTimeFormatter dtoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final LocalDateTime startDateTime = LocalDateTime.of(2024, 10, 14, 0, 0); //관찰 시작일
    private final LocalDateTime endDateTime = LocalDateTime.now(); // 현재 시간까지

    @Value("${app.log.directory:/spring-boot/log}") //app.log.directory 가 설정되지 않을 경우 기본 경로 설정
    private String LOG_DIRECTORY;

    @GetMapping("/home")
    public String adminHome(HttpSession session, Model model)
    {
        User user = (User) session.getAttribute("user");
        if(user == null) return "redirect:/login";


        // LocalDate로 변환하여 두 날짜의 차이를 구함
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDateTime.toLocalDate(), endDateTime.toLocalDate()) + 1; // 관찰 시작일부터 1일차이기 때문에 1을 더함

        List<AdminHomeDTO> dtoList = new ArrayList<>();
        double totalPercent = 0.0;
        int countUser = 0;
        DecimalFormat df = new DecimalFormat("#.##"); // 소수점 두 자리까지만 표현
        for(User u : userService.findAllUser())
        {
            AdminHomeDTO newDto = new AdminHomeDTO();
            newDto.setId(u.getId());
            newDto.setRole(u.getRole());
            newDto.setName(u.getName());
            newDto.setAge(u.getAge());
            newDto.setBottleId(u.getBottleId());
            int notificationSize = notificationService.findNotificationByUser(u).size();
            newDto.setTotalMedicine(notificationSize);

            if (daysBetween == 0 || notificationSize == 0) newDto.setPercent(0); // 알림이 없는 경우 0%로 설정
            else
            {
                int logSize = medicationLogService.findByUserAndTimeBetween(u, startDateTime, endDateTime).size();
                double percent = (logSize / (daysBetween * (double) notificationSize)) * 100;
                newDto.setLogSize(logSize);
                newDto.setPercent(Double.parseDouble(df.format(percent)));

                if (newDto.getRole() != Role.admin)
                {
                    totalPercent += newDto.getPercent();
                    ++countUser;
                }
            }
            dtoList.add(newDto);
        }
        totalPercent = countUser > 0 ? Double.parseDouble(df.format(totalPercent / countUser)) : 0; //관리자(테스트) 계정의 복약 순응도는 반영 안함

        List<reportAdminDTO> reportList = new ArrayList<>();

        for(Report r : reportService.findAllReport())
        {
            reportAdminDTO newReportDto = new reportAdminDTO();
            newReportDto.setId(r.getId());
            newReportDto.setUserName(r.getUser().getName());
            newReportDto.setTitle(r.getTitle());
            newReportDto.setCreatAt(r.getCreateAt().format(dtoFormatter));
            reportList.add(newReportDto);
        }

        File logDir = new File(LOG_DIRECTORY);
        List<String> logFiles = new ArrayList<>();

        // log 디렉토리에서 모든 로그 파일을 찾기
        if (logDir.exists() && logDir.isDirectory())
        {
            for (File file : logDir.listFiles()) if (file.isFile() && file.getName().startsWith("info-")) logFiles.add(file.getName());
        }
        // 로그 파일 이름을 날짜와 순번에 따라 정렬
        logFiles.sort(Comparator.comparing((String fileName) -> {
            String datePart = fileName.substring(5, 15); // 날짜 부분 추출
            return LocalDate.parse(datePart, logFormatter);
        }).thenComparingInt(fileName -> {
            String[] parts = fileName.split("\\."); // 순번 추출
            return Integer.parseInt(parts[1]);
        }));

        model.addAttribute("logFiles", logFiles);
        model.addAttribute("allReport", reportList);
        model.addAttribute("userInfo", dtoList);
        model.addAttribute("totalPercent", totalPercent);
        model.addAttribute("startDateTime", startDateTime);
        model.addAttribute("daysBetween", daysBetween);
        return "adminView/home";
    }
}
