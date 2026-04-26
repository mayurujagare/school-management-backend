package com.schoolmanagement.module.academic.repository;

import com.schoolmanagement.module.academic.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findBySchoolIdAndIsActiveTrueOrderByNameAsc(UUID schoolId);

    List<Subject> findBySchoolIdOrderByNameAsc(UUID schoolId);

    Optional<Subject> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndCodeIgnoreCase(UUID schoolId, String code);

    boolean existsBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);
}