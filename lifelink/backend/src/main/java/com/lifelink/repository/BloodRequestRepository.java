package com.lifelink.repository;

import com.lifelink.model.BloodRequest;
import com.lifelink.model.RequestStatus;
import com.lifelink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BloodRequestRepository extends JpaRepository<BloodRequest, Long> {

    List<BloodRequest> findByStatus(RequestStatus status);

    List<BloodRequest> findByRequesterOrderByCreatedAtDesc(User requester);

    List<BloodRequest> findByStatusAndRequiredByBefore(RequestStatus status, LocalDate date);

    List<BloodRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(RequestStatus status);
}
