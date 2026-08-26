package com.recoverai.recoverai.service;

import com.recoverai.recoverai.entity.FailedMandate;

import java.time.LocalDateTime;
import java.util.List;

public interface RetryStrategyService {
    List<LocalDateTime> generate(FailedMandate mandate);
}
