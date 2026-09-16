package com.shiftcalendar.app.logic;

import com.shiftcalendar.app.model.DayStatus;
import com.shiftcalendar.app.model.ShiftConfig;
import com.shiftcalendar.app.model.ShiftType;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShiftCalculatorTest {

    private ShiftConfig config2x2() {
        LocalDate start = LocalDate.of(2026, 1, 1); // 1 января 2026 = индекс 0 = Работа
        ShiftType work = new ShiftType("work", "Work", "W", 0, true);
        ShiftType rest = new ShiftType("rest", "Rest", "R", 0, false);
        List<ShiftType> cycle = Arrays.asList(work, work, rest, rest);
        return new ShiftConfig(start, cycle);
    }

    @Test
    public void startDate_isWorkDay() {
        ShiftConfig config = config2x2();
        DayStatus status = ShiftCalculator.getStatus(config, LocalDate.of(2026, 1, 1));
        assertTrue(status.getShiftType().isWork());
        assertEquals(0, status.getCycleIndex());
    }

    @Test
    public void secondDay_isWorkDay() {
        ShiftConfig config = config2x2();
        DayStatus status = ShiftCalculator.getStatus(config, LocalDate.of(2026, 1, 2));
        assertTrue(status.getShiftType().isWork());
        assertEquals(1, status.getCycleIndex());
    }

    @Test
    public void thirdAndFourthDay_isRestDay() {
        ShiftConfig config = config2x2();
        assertFalse(ShiftCalculator.getStatus(config, LocalDate.of(2026, 1, 3)).getShiftType().isWork());
        assertFalse(ShiftCalculator.getStatus(config, LocalDate.of(2026, 1, 4)).getShiftType().isWork());
    }

    @Test
    public void fifthDay_cycleRepeats_isWorkDayAgain() {
        ShiftConfig config = config2x2();
        DayStatus status = ShiftCalculator.getStatus(config, LocalDate.of(2026, 1, 5));
        assertTrue(status.getShiftType().isWork());
        assertEquals(0, status.getCycleIndex());
    }

    @Test
    public void dateBeforeStart_negativeDiff_handledCorrectly() {
        ShiftConfig config = config2x2();
        // 31 декабря 2025 — за 1 день до точки отсчёта. Индекс должен быть 3 (последний в цикле = REST).
        DayStatus status = ShiftCalculator.getStatus(config, LocalDate.of(2025, 12, 31));
        assertEquals(3, status.getCycleIndex());
        assertFalse(status.getShiftType().isWork());
    }
}
