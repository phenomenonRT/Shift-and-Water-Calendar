package com.shiftcalendar.app.model;

import java.time.LocalDate;

/**
 * Рассчитанный статус конкретного дня.
 */
public class DayStatus {

    private final LocalDate date;
    private final ShiftType shiftType;
    private final boolean isWaterDay;
    private final int cycleIndex;

    public DayStatus(LocalDate date, ShiftType shiftType, boolean isWaterDay, int cycleIndex) {
        this.date = date;
        this.shiftType = shiftType;
        this.isWaterDay = isWaterDay;
        this.cycleIndex = cycleIndex;
    }

    public LocalDate getDate() {
        return date;
    }

    public ShiftType getShiftType() {
        return shiftType;
    }

    public boolean isWaterDay() {
        return isWaterDay;
    }

    public int getCycleIndex() {
        return cycleIndex;
    }
}
