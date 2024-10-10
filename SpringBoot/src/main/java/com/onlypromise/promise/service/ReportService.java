package com.onlypromise.promise.service;

import com.onlypromise.promise.domain.Report;
import com.onlypromise.promise.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;

    public void save(Report report)
    {
        reportRepository.save(report);
    }

    public List<Report> findAllReport()
    {
        return reportRepository.findAll();
    }

    public Optional<Report> findById(long id)
    {
        return reportRepository.findById(id);
    }
}
