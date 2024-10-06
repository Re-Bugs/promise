package com.onlypromise.promise.repository;

import com.onlypromise.promise.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserId(String userId);

    boolean existsByNickName(String nickName);

    Optional<User> findByBottleId(String bottleId);  // bottle_id로 사용자 찾기

    List<User> findAll(); // 모든 유저 리턴
}
