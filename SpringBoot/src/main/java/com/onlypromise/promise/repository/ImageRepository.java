package com.onlypromise.promise.repository;

import com.onlypromise.promise.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
