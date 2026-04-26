package com.schoolmanagement.module.staff.repository;

import com.schoolmanagement.module.staff.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StaffRepository extends JpaRepository<Staff, UUID> {

    Optional<Staff> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<Staff> findByUser_IdAndSchoolId(UUID userId, UUID schoolId);

    boolean existsBySchoolIdAndEmployeeCode(UUID schoolId, String employeeCode);

    @Query("""
        SELECT s FROM Staff s
        JOIN s.user u
        WHERE s.schoolId = :schoolId
        AND s.isActive = :isActive
        AND (
            :search IS NULL
            OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(u.email)     LIKE LOWER(CONCAT('%',:search,'%'))
            OR LOWER(s.employeeCode) LIKE LOWER(CONCAT('%',:search,'%'))
        )
        ORDER BY u.firstName ASC
    """)
    Page<Staff> searchStaff(UUID schoolId,
                             Boolean isActive,
                             String search,
                             Pageable pageable);
}