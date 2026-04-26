package com.schoolmanagement.module.staff.util;

import com.schoolmanagement.module.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmployeeCodeGenerator {

    private final StaffRepository staffRepository;

    // Format: EMP0001, EMP0002 ...
    public String generate(UUID schoolId) {
        long count = staffRepository.count() + 1;
        return String.format("EMP%04d", count);
    }
}