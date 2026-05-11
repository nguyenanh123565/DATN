package com.smarthome.repository;

import com.smarthome.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByRole(User.Role role, Pageable pageable);
    Page<User> findByLoyaltyRank(User.LoyaltyRank loyaltyRank, Pageable pageable);
    Page<User> findByRoleAndLoyaltyRank(User.Role role, User.LoyaltyRank loyaltyRank, Pageable pageable);
}
