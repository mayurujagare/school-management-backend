package com.schoolmanagement.module.student.util;

import com.schoolmanagement.module.school.service.SchoolConfigService;
import com.schoolmanagement.module.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdmissionNumberGenerator {

    private final StudentRepository   studentRepository;
    private final SchoolConfigService configService;

    // ── Generate next admission number ────────────────────────
    // Format: {PREFIX}{YEAR}{SEQUENCE}
    // Example: ADM20240001, ADM20240002 ...
    // Prefix is taken from school config (receipt_prefix or default 'ADM')
    public String generate(UUID schoolId) {

        String prefix = configService
                .getConfigOrDefault(schoolId, "admission_prefix", "ADM");

        int    year     = Year.now().getValue();
        String yearStr  = String.valueOf(year);

        // Find latest admission number to determine next sequence
        String latest = studentRepository
                .findLatestAdmissionNo(schoolId)
                .orElse(null);

        int nextSeq = 1;

        if (latest != null && latest.contains(yearStr)) {
            try {
                // Extract sequence from end of last admission number
                String seqPart = latest.substring(
                        latest.indexOf(yearStr) + yearStr.length());
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException e) {
                log.warn("Could not parse sequence from admission no: {}", latest);
                nextSeq = 1;
            }
        }

        // Format: PREFIX + YEAR + 4-digit sequence
        return String.format("%s%s%04d", prefix, yearStr, nextSeq);
    }
}