package com.recoverai.recoverai.service;

import com.recoverai.recoverai.dto.AiOperationalContext;

public interface AiContextService {
    AiOperationalContext buildContext(String question);
}
