package com.lifelink.controller;

import com.lifelink.dto.MiscDtos.ResponseDto;
import com.lifelink.dto.RequestDtos.StatusUpdate;
import com.lifelink.service.ResponseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/responses")
public class ResponseController {

    private final ResponseService responseService;

    public ResponseController(ResponseService responseService) {
        this.responseService = responseService;
    }

    @GetMapping("/mine")
    public List<ResponseDto> mine() {
        return responseService.mine();
    }

    @PutMapping("/{id}/status")
    public ResponseDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdate body) {
        return responseService.updateStatus(id, body.status());
    }
}
