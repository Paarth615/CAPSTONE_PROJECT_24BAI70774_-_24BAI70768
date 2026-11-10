package com.lifelink.service;

import com.lifelink.dto.UserDtos.ProfileUpdate;
import com.lifelink.dto.UserDtos.UserDto;
import com.lifelink.exception.ApiException;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.repository.UserRepository;
import com.lifelink.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private final UserRepository users;
    private final CurrentUser currentUser;
    private final EligibilityService eligibility;
    private final DtoMapper mapper;

    public ProfileService(UserRepository users, CurrentUser currentUser, EligibilityService eligibility,
                          DtoMapper mapper) {
        this.users = users;
        this.currentUser = currentUser;
        this.eligibility = eligibility;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        return mapper.toUserDto(currentUser.get());
    }

    public UserDto update(ProfileUpdate f) {
        User u = currentUser.get();
        u.setName(f.name().trim());
        u.setPhone(f.phone());
        u.setCity(f.city().trim());
        u.setLatitude(f.latitude());
        u.setLongitude(f.longitude());

        if (u.getRole() == Role.DONOR) {
            if (f.bloodGroup() == null) {
                throw ApiException.badRequest("Blood group is required for donors");
            }
            eligibility.requireDonorAge(f.dateOfBirth());
            eligibility.requireSaneWeight(f.weightKg());
            u.setBloodGroup(f.bloodGroup());
            u.setDateOfBirth(f.dateOfBirth());
            u.setWeightKg(f.weightKg());
            u.setLastDonationDate(f.lastDonationDate());
        }
        return mapper.toUserDto(users.save(u));
    }

    public UserDto setAvailability(boolean available) {
        User u = currentUser.get();
        if (u.getRole() != Role.DONOR) {
            throw ApiException.badRequest("Only donors have an availability setting");
        }
        u.setAvailable(available);
        return mapper.toUserDto(users.save(u));
    }
}
