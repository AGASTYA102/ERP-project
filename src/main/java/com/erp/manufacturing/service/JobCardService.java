package com.erp.manufacturing.service;

import com.erp.manufacturing.entity.JobCard;

import java.util.Optional;

public interface JobCardService {
    JobCard createJobCard(JobCard jobCard);
    Optional<JobCard> getJobCardByOrderId(Long orderId);
}
