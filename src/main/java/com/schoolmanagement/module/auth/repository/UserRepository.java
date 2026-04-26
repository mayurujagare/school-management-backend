package com.schoolmanagement.module.auth.repository;

import com.schoolmanagement.module.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobile(String mobile);

    @Query("""
        SELECT u FROM User u
        WHERE (u.email = :identifier OR u.mobile = :identifier)
        AND u.schoolId = :schoolId
        AND u.isActive = true
    """)
    Optional<User> findByIdentifierAndSchoolId(String identifier, UUID schoolId);

    @Query("""
        SELECT u FROM User u
        WHERE (u.email = :identifier OR u.mobile = :identifier)
        AND u.isActive = true
    """)
    Optional<User> findByIdentifier(String identifier);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);
}