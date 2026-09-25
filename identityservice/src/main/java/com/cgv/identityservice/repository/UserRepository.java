package com.cgv.identityservice.repository;

import com.cgv.identityservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    @Override
    @EntityGraph(attributePaths = {"membershipTier"})
    Optional<User> findById(String id);

    @EntityGraph(attributePaths = {"membershipTier"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"membershipTier"})
    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    @EntityGraph(attributePaths = {"membershipTier"})
    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(:keyword) OR LOWER(u.email) LIKE LOWER(:keyword) OR (u.phone IS NOT NULL AND u.phone LIKE :keyword)")
    Page<User> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
