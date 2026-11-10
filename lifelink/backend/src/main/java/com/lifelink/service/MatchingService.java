package com.lifelink.service;

import com.lifelink.dto.RequestDtos.MatchDto;
import com.lifelink.model.*;
import com.lifelink.repository.DonationResponseRepository;
import com.lifelink.repository.UserRepository;
import com.lifelink.util.GeoUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Smart matching: ranks the donors who can safely help with a request.
 *
 * Hard filters (a donor failing any of these is never shown):
 *   - active donor account, marked available
 *   - blood group is ABO/Rh compatible with the patient
 *   - passes eligibility (age, weight, donation cool-down)
 *   - is not the person who raised the request
 *
 * Score out of 100 for the donors who pass:
 *   blood group  40  exact match, 25 when merely compatible
 *   proximity    30  by distance when both sides have coordinates, else same-city = 24
 *   rest         20  10 as soon as the cool-down ends, rising to 20 after another 90 days; 16 if never donated
 *   reliability  10  3 points per donation completed through LifeLink, capped at 10
 */
@Service
@Transactional(readOnly = true)
public class MatchingService {

    private final UserRepository users;
    private final DonationResponseRepository responses;
    private final EligibilityService eligibility;

    public MatchingService(UserRepository users, DonationResponseRepository responses,
                           EligibilityService eligibility) {
        this.users = users;
        this.responses = responses;
        this.eligibility = eligibility;
    }

    public List<MatchDto> findMatches(BloodRequest request, int limit) {
        return users.findByRoleAndEnabledTrueAndAvailableTrue(Role.DONOR).stream()
                .filter(d -> !d.getId().equals(request.getRequester().getId()))
                .filter(d -> d.getBloodGroup() != null && d.getBloodGroup().canDonateTo(request.getBloodGroup()))
                .filter(eligibility::isEligible)
                .map(d -> score(d, request))
                .sorted(Comparator.comparingInt(MatchDto::score).reversed()
                        .thenComparingDouble(m -> m.distanceKm() == null ? Double.MAX_VALUE : m.distanceKm()))
                .limit(limit)
                .toList();
    }

    MatchDto score(User donor, BloodRequest req) {
        List<String> reasons = new ArrayList<>();

        // 1. Blood group (max 40)
        boolean exact = donor.getBloodGroup() == req.getBloodGroup();
        int blood = exact ? 40 : 25;
        if (exact) {
            reasons.add("Exact blood group match (" + donor.getBloodGroup().getLabel() + ")");
        } else if (donor.getBloodGroup() == BloodGroup.O_NEG) {
            reasons.add("Universal donor (O-) can give to any group");
        } else {
            reasons.add("Compatible: " + donor.getBloodGroup().getLabel() + " can donate to "
                    + req.getBloodGroup().getLabel());
        }

        // 2. Proximity (max 30)
        Double distance = null;
        int location;
        if (donor.getLatitude() != null && donor.getLongitude() != null
                && req.getLatitude() != null && req.getLongitude() != null) {
            distance = GeoUtil.round1(GeoUtil.distanceKm(donor.getLatitude(), donor.getLongitude(),
                    req.getLatitude(), req.getLongitude()));
            if (distance <= 5) {
                location = 30;
            } else if (distance <= 15) {
                location = 26;
            } else if (distance <= 50) {
                location = 20;
            } else if (distance <= 150) {
                location = 10;
            } else {
                location = 3;
            }
            reasons.add(distance + " km from the hospital");
        } else if (donor.getCity() != null && donor.getCity().equalsIgnoreCase(req.getCity())) {
            location = 24;
            reasons.add("Lives in " + req.getCity());
        } else {
            location = 0;
            reasons.add("Lives in a different city");
        }

        // 3. Rest since last donation (max 20)
        int rest;
        LocalDate last = donor.getLastDonationDate();
        if (last == null) {
            rest = 16;
            reasons.add("Has not donated before and is ready to give");
        } else {
            long days = ChronoUnit.DAYS.between(last, LocalDate.now());
            long over = Math.max(0, days - eligibility.getCooldownDays());
            rest = 10 + (int) Math.min(10, over * 10 / 90.0);
            reasons.add("Last donated " + days + " days ago");
        }

        // 4. Reliability (max 10)
        long completed = responses.countByDonorAndStatus(donor, ResponseStatus.COMPLETED);
        int reliability = (int) Math.min(10, completed * 3);
        if (completed > 0) {
            reasons.add(completed + (completed == 1 ? " donation" : " donations") + " completed on LifeLink");
        }

        int total = blood + location + rest + reliability;
        return new MatchDto(donor.getId(), donor.getName(), donor.getBloodGroup(), donor.getCity(),
                donor.getPhone(), total, distance, donor.getLastDonationDate(), completed, exact, reasons);
    }
}
