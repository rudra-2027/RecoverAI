package com.recoverai.recoverai.service;

import com.recoverai.recoverai.entity.FailedMandate;

public interface ProbabilityEngineService {
    int calculate(FailedMandate mandate);
}
