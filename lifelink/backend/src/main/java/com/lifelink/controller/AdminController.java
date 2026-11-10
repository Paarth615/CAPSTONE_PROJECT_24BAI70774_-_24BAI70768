package com.lifelink.controller;

import com.lifelink.dto.MiscDtos.AdminStats;
import com.lifelink.dto.RequestDtos.RequestDto;
import com.lifelink.dto.UserDtos.EnabledUpdate;
import com.lifelink.dto.UserDtos.UserDto;
import com.lifelink.service.AdminService;
import com.lifelink.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** All endpoints here are restricted to ROLE_ADMIN in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final StatsService statsService;

    public AdminController(AdminService adminService, StatsService statsService) {
        this.adminService = adminService;
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public AdminStats stats() {
        return statsService.adminStats();
    }

    @GetMapping("/users")
    public List<UserDto> users() {
        return adminService.listUsers();
    }

    @PutMapping("/users/{id}/enabled")
    public UserDto setEnabled(@PathVariable Long id, @RequestBody EnabledUpdate body) {
        return adminService.setEnabled(id, body.enabled());
    }

    @GetMapping("/requests")
    public List<RequestDto> requests() {
        return adminService.listRequests();
    }

    @DeleteMapping("/requests/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        adminService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
}
