package com.onlypromise.promise.service;

import com.onlypromise.promise.domain.Image;
import com.onlypromise.promise.domain.Report;
import com.onlypromise.promise.repository.ImageRepository;
import com.onlypromise.promise.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final ImageRepository imageRepository;

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

    public void saveImage(Image image)
    {
        imageRepository.save(image);
    }
}
