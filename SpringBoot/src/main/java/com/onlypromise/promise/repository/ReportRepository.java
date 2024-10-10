package com.onlypromise.promise.repository;

import com.onlypromise.promise.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
}
