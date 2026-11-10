package com.lifelink.service;

import com.lifelink.dto.RequestDtos.RequestDto;
import com.lifelink.dto.UserDtos.UserDto;
import com.lifelink.exception.ApiException;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.UserRepository;
import com.lifelink.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository users;
    private final BloodRequestRepository requests;
    private final RequestService requestService;
    private final CurrentUser currentUser;
    private final DtoMapper mapper;

    public AdminService(UserRepository users, BloodRequestRepository requests, RequestService requestService,
                        CurrentUser currentUser, DtoMapper mapper) {
        this.users = users;
        this.requests = requests;
        this.requestService = requestService;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers() {
        return users.findAllByOrderByCreatedAtDesc().stream().map(mapper::toUserDto).toList();
    }

    public UserDto setEnabled(Long userId, boolean enabled) {
        User admin = currentUser.get();
        User target = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (target.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Administrator accounts cannot be disabled");
        }
        if (target.getId().equals(admin.getId())) {
            throw ApiException.badRequest("You cannot disable your own account");
        }
        target.setEnabled(enabled);
        log.info("Admin {} set user {} enabled={}", admin.getId(), userId, enabled);
        return mapper.toUserDto(users.save(target));
    }

    @Transactional(readOnly = true)
    public List<RequestDto> listRequests() {
        User admin = currentUser.get();
        return requests.findAllByOrderByCreatedAtDesc().stream().map(r -> mapper.toRequestDto(r, admin)).toList();
    }

    public void deleteRequest(Long id) {
        requestService.adminDelete(id);
    }
}
