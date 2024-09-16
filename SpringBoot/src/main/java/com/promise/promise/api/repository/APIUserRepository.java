package com.promise.promise.api.repository;

import com.promise.promise.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface APIUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByBottleId(String bottleId);  // bottle_id로 사용자 찾기
}