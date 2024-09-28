package com.onlypromise.promise.repository.web;

import com.onlypromise.promise.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUserId(String userId);

    Optional<User> findByUserId(String userId);

    boolean existsByNickName(String nickName);

    Optional<User> findByBottleId(String bottleId);  // bottle_id로 사용자 찾기
}
