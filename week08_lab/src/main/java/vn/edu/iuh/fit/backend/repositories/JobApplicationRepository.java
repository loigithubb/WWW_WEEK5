/*
 * @ (#) JobApplicationReppository.java       1.0     17/11/2024
 *
 * Copyright (c) 2024 IUH. All rights reserved.
 */

package vn.edu.iuh.fit.backend.repositories;
/*
 * @description:
 * @author: Nguyen Thanh Nhut
 * @date: 17/11/2024
 * @version:    1.0
 */

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.backend.models.Candidate;
import vn.edu.iuh.fit.backend.models.JobApplication;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    Page<JobApplication> findByJobId(Long jobId, Pageable pageable);

    Page<JobApplication> findByJob_CompanyId(Long companyId, Pageable pageable);
    Page<JobApplication> findByJob_CompanyIdAndCandidate_FullNameContaining(Long companyId, String fullName, Pageable pageable);
    Page<JobApplication> findByJob_IdAndCandidate_FullNameContaining(Long jobId, String fullName, Pageable pageable);
}
