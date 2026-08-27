package com.recoverai.recoverai.controller;

import com.recoverai.recoverai.dto.SimulatorRecoveryRequest;
import com.recoverai.recoverai.dto.SimulatorRecoveryResponse;
import com.recoverai.recoverai.service.SimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/simulator")
public class SimulatorController {
    private final SimulationService simulationService;

    @PostMapping("/recovery")
    @ResponseStatus(HttpStatus.CREATED)
    public SimulatorRecoveryResponse recover(@Valid @RequestBody SimulatorRecoveryRequest request) {
        return simulationService.recover(request.mandate());
    }
}
