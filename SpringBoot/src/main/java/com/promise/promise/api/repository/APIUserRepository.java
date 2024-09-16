package com.promise.promise.api.repository;

import com.promise.promise.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface APIUserRepository extends JpaRepository<User, Long> {
}
