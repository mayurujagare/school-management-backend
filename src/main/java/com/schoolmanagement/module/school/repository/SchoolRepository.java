package com.schoolmanagement.module.school.repository;

import com.schoolmanagement.module.school.entity.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {

    Optional<School> findByIdAndIsActiveTrue(UUID id);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // Super admin — list all schools with filters
    @Query("""
        SELECT s FROM School s
        WHERE (:search IS NULL
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(s.city) LIKE LOWER(CONCAT('%', :search, '%')))
        AND   (:isActive IS NULL OR s.isActive = :isActive)
        ORDER BY s.createdAt DESC
    """)
    Page<School> findAllWithFilters(String search,
                                    Boolean isActive,
                                    Pageable pageable);
}