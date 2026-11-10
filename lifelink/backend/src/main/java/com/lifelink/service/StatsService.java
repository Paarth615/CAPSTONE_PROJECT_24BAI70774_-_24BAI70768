package com.lifelink.service;

import com.lifelink.dto.MiscDtos.AdminStats;
import com.lifelink.dto.MiscDtos.PublicStats;
import com.lifelink.model.*;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationResponseRepository;
import com.lifelink.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final UserRepository users;
    private final BloodRequestRepository requests;
    private final DonationResponseRepository responses;

    public StatsService(UserRepository users, BloodRequestRepository requests,
                        DonationResponseRepository responses) {
        this.users = users;
        this.requests = requests;
        this.responses = responses;
    }

    public PublicStats publicStats() {
        Map<String, Long> byGroup = new LinkedHashMap<>();
        for (BloodGroup g : BloodGroup.values()) {
            byGroup.put(g.getLabel(), 0L);
        }
        for (Object[] row : users.countAvailableDonorsByGroup(Role.DONOR)) {
            byGroup.put(((BloodGroup) row[0]).getLabel(), (Long) row[1]);
        }
        return new PublicStats(
                users.countByRole(Role.DONOR),
                users.countByRoleAndEnabledTrueAndAvailableTrue(Role.DONOR),
                requests.countByStatus(RequestStatus.OPEN),
                requests.countByStatus(RequestStatus.FULFILLED),
                responses.countByStatus(ResponseStatus.COMPLETED),
                users.countDistinctCities(),
                byGroup);
    }

    public AdminStats adminStats() {
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role r : Role.values()) {
            byRole.put(r.name(), users.countByRole(r));
        }
        Map<String, Long> byRequestStatus = new LinkedHashMap<>();
        for (RequestStatus s : RequestStatus.values()) {
            byRequestStatus.put(s.name(), requests.countByStatus(s));
        }
        Map<String, Long> byResponseStatus = new LinkedHashMap<>();
        for (ResponseStatus s : ResponseStatus.values()) {
            byResponseStatus.put(s.name(), responses.countByStatus(s));
        }
        Map<String, Long> donorsByGroup = new LinkedHashMap<>();
        for (BloodGroup g : BloodGroup.values()) {
            donorsByGroup.put(g.getLabel(), users.countByBloodGroup(g));
        }
        return new AdminStats(byRole, byRequestStatus, byResponseStatus, donorsByGroup,
                users.count(), requests.count());
    }
}
