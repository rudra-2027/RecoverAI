package com.recoverai.recoverai.service.impl;

import com.recoverai.recoverai.entity.FailedMandate;
import com.recoverai.recoverai.service.RetryStrategyService;
import com.recoverai.recoverai.service.RuntimeSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RetryStrategyServiceImpl implements RetryStrategyService {
    private final RuntimeSettingsService runtimeSettingsService;

    @Override
    public List<LocalDateTime> generate(FailedMandate mandate) {
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> schedule = switch (mandate.getFailureReason()) {
            case "INSUFFICIENT_BALANCE" -> List.of(
                    now.plusDays(1),
                    nextSalaryDay(),
                    now.plusDays(2),
                    now.plusDays(5),
                    now.plusDays(10));
            case "BANK_SERVER_DOWN" -> List.of(
                    now.plusHours(1),
                    now.plusHours(4),
                    now.plusDays(1).with(LocalTime.of(9, 0)));
            case "NPCI_TIMEOUT" -> List.of(
                    now.plusMinutes(30),
                    now.plusHours(2),
                    now.plusHours(6));
            case "MANDATE_REVOKED", "MANDATE_EXPIRED", "CARD_EXPIRED" -> List.of();
            default -> List.of(now.plusDays(1));
        };

        return schedule.stream()
                .map(this::avoidPeakWindow)
                .toList();
    }

    private LocalDateTime nextSalaryDay() {
        LocalDate date = LocalDate.now();
        LocalDate salaryDay = date.withDayOfMonth(Math.min(1, date.lengthOfMonth())).plusMonths(1);
        return salaryDay.atTime(9, 0);
    }

    private LocalDateTime avoidPeakWindow(LocalDateTime retryAt) {
        int hour = retryAt.getHour();
        if (hour >= runtimeSettingsService.peakStartHour() && hour < runtimeSettingsService.peakEndHour()) {
            return retryAt.withHour(runtimeSettingsService.peakEndHour()).withMinute(0).withSecond(0).withNano(0);
        }
        return retryAt;
    }
}
