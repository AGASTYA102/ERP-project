package com.erp.manufacturing.service.impl;

import com.erp.manufacturing.entity.JobCard;
import com.erp.manufacturing.repository.JobCardRepository;
import com.erp.manufacturing.service.JobCardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class JobCardServiceImpl implements JobCardService {

    private final JobCardRepository jobCardRepository;

    public JobCardServiceImpl(JobCardRepository jobCardRepository) {
        this.jobCardRepository = jobCardRepository;
    }

    @Override
    public JobCard createJobCard(JobCard jobCard) {
        // Prevent duplicate creation
        if (jobCard.getOrder() != null && jobCard.getOrder().getId() != null) {
            Optional<JobCard> existing = jobCardRepository.findByOrderId(jobCard.getOrder().getId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return jobCardRepository.save(jobCard);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobCard> getJobCardByOrderId(Long orderId) {
        return jobCardRepository.findByOrderId(orderId);
    }
}
