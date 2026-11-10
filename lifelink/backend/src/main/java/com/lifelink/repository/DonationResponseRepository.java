package com.lifelink.repository;

import com.lifelink.model.BloodRequest;
import com.lifelink.model.DonationResponse;
import com.lifelink.model.ResponseStatus;
import com.lifelink.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonationResponseRepository extends JpaRepository<DonationResponse, Long> {

    List<DonationResponse> findByDonorOrderByCreatedAtDesc(User donor);

    List<DonationResponse> findByRequestOrderByCreatedAtDesc(BloodRequest request);

    Optional<DonationResponse> findByRequestAndDonor(BloodRequest request, User donor);

    long countByRequest(BloodRequest request);

    long countByDonorAndStatus(User donor, ResponseStatus status);

    long countByStatus(ResponseStatus status);
}
