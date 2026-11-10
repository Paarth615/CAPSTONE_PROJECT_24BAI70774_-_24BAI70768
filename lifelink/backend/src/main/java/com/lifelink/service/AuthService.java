package com.lifelink.service;

import com.lifelink.dto.AuthDtos.AuthResponse;
import com.lifelink.dto.AuthDtos.LoginRequest;
import com.lifelink.dto.AuthDtos.RegisterRequest;
import com.lifelink.exception.ApiException;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.repository.UserRepository;
import com.lifelink.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EligibilityService eligibility;
    private final DtoMapper mapper;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt,
                       EligibilityService eligibility, DtoMapper mapper) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.eligibility = eligibility;
        this.mapper = mapper;
    }

    public AuthResponse register(RegisterRequest f) {
        if (f.role() == Role.ADMIN) {
            throw ApiException.badRequest("Choose Donor or Requester");
        }
        String email = f.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }

        User u = new User();
        u.setName(f.name().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(f.password()));
        u.setRole(f.role());
        u.setPhone(f.phone());
        u.setCity(f.city().trim());
        u.setLatitude(f.latitude());
        u.setLongitude(f.longitude());

        if (f.role() == Role.DONOR) {
            if (f.bloodGroup() == null) {
                throw ApiException.badRequest("Blood group is required for donors");
            }
            eligibility.requireDonorAge(f.dateOfBirth());
            eligibility.requireSaneWeight(f.weightKg());
            u.setBloodGroup(f.bloodGroup());
            u.setDateOfBirth(f.dateOfBirth());
            u.setWeightKg(f.weightKg());
        }

        users.save(u);
        log.info("Registered new {} account (id={})", u.getRole(), u.getId());
        return new AuthResponse(jwt.generateToken(u), mapper.toUserDto(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest f) {
        User u = users.findByEmailIgnoreCase(f.email().trim())
                .orElseThrow(() -> ApiException.unauthorized("Email or password is incorrect"));
        if (!encoder.matches(f.password(), u.getPassword())) {
            throw ApiException.unauthorized("Email or password is incorrect");
        }
        if (!u.isEnabled()) {
            throw ApiException.forbidden("This account has been disabled. Contact an administrator.");
        }
        log.info("User {} signed in", u.getId());
        return new AuthResponse(jwt.generateToken(u), mapper.toUserDto(u));
    }
}
