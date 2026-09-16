package com.shiftcalendar.app.logic;

import com.shiftcalendar.app.model.ShiftType;
import com.shiftcalendar.app.model.DayStatus;
import com.shiftcalendar.app.model.ShiftConfig;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Основная логика расчёта: определяет статус (работа/выходной/вода)
 * для любой даты относительно точки отсчёта и массива цикла.
 *
 * Индекс = (ЦелеваяДата - ДатаНачала) % ДлинаЦикла,
 * с корректной обработкой отрицательных дат (до старта отсчёта).
 */
public final class ShiftCalculator {

    private ShiftCalculator() {
    }

    public static DayStatus getStatus(ShiftConfig config, LocalDate targetDate) {
        int cycleLength = config.getCycleLength();
        long diffDays = ChronoUnit.DAYS.between(config.getStartDate(), targetDate);

        // Java % может вернуть отрицательный результат для отрицательного diffDays.
        // Приводим индекс в диапазон [0, cycleLength - 1].
        int rawIndex = (int) (diffDays % cycleLength);
        int index = ((rawIndex % cycleLength) + cycleLength) % cycleLength;

        ShiftType type = config.getCycle().get(index);
        boolean isWaterDay = false;
        if (config.getWaterDays() != null && index < config.getWaterDays().size()) {
            isWaterDay = config.getWaterDays().get(index);
        }

        return new DayStatus(targetDate, type, isWaterDay, index);
    }
}
