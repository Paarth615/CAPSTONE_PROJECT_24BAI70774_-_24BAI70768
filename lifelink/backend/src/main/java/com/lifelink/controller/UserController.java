package com.lifelink.controller;

import com.lifelink.dto.UserDtos.AvailabilityUpdate;
import com.lifelink.dto.UserDtos.ProfileUpdate;
import com.lifelink.dto.UserDtos.UserDto;
import com.lifelink.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserDto me() {
        return profileService.me();
    }

    @PutMapping
    public UserDto update(@Valid @RequestBody ProfileUpdate body) {
        return profileService.update(body);
    }

    @PutMapping("/availability")
    public UserDto availability(@RequestBody AvailabilityUpdate body) {
        return profileService.setAvailability(body.available());
    }
}
