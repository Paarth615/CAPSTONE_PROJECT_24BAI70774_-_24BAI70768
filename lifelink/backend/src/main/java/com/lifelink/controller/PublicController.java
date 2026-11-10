package com.lifelink.controller;

import com.lifelink.dto.MiscDtos.PublicStats;
import com.lifelink.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final StatsService statsService;

    public PublicController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public PublicStats stats() {
        return statsService.publicStats();
    }
}
