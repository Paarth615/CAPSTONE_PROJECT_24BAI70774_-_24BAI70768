package com.lifelink.controller;

import com.lifelink.dto.MiscDtos.RespondForm;
import com.lifelink.dto.MiscDtos.ResponseDto;
import com.lifelink.dto.RequestDtos.MatchDto;
import com.lifelink.dto.RequestDtos.RequestDto;
import com.lifelink.dto.RequestDtos.RequestForm;
import com.lifelink.dto.RequestDtos.StatusUpdate;
import com.lifelink.service.RequestService;
import com.lifelink.service.ResponseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;
    private final ResponseService responseService;

    public RequestController(RequestService requestService, ResponseService responseService) {
        this.requestService = requestService;
        this.responseService = responseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestDto create(@Valid @RequestBody RequestForm body) {
        return requestService.create(body);
    }

    @GetMapping
    public List<RequestDto> list(@RequestParam(required = false) String blood,
                                 @RequestParam(required = false) String city,
                                 @RequestParam(required = false) String urgency,
                                 @RequestParam(defaultValue = "false") boolean forMe) {
        return requestService.list(blood, city, urgency, forMe);
    }

    @GetMapping("/mine")
    public List<RequestDto> mine() {
        return requestService.mine();
    }

    @GetMapping("/{id}")
    public RequestDto get(@PathVariable Long id) {
        return requestService.get(id);
    }

    @GetMapping("/{id}/matches")
    public List<MatchDto> matches(@PathVariable Long id) {
        return requestService.matches(id);
    }

    @PostMapping("/{id}/notify-matches")
    public Map<String, Integer> notifyMatches(@PathVariable Long id) {
        return Map.of("notified", requestService.notifyMatches(id));
    }

    @PutMapping("/{id}/status")
    public RequestDto updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdate body) {
        return requestService.updateStatus(id, body.status());
    }

    @PostMapping("/{id}/respond")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseDto respond(@PathVariable Long id, @Valid @RequestBody(required = false) RespondForm body) {
        return responseService.respond(id, body);
    }

    @GetMapping("/{id}/responses")
    public List<ResponseDto> responses(@PathVariable Long id) {
        return responseService.forRequest(id);
    }
}
