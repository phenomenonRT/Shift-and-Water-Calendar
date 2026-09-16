package com.shiftcalendar.app.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Конфигурация циклического графика работы.
 *
 * startDate  — точка отсчёта (любая дата, которая соответствует индексу 0 в цикле).
 * cycle      — массив состояний дня, например [WORK, WORK, REST, REST] для графика 2/2.
 * waterDayIndex — индекс дня в цикле (0..cycle.size()-1), в который привозят воду.
 *                 -1, если события "вода" в графике нет.
 */
public class ShiftConfig {

    private final LocalDate startDate;
    private final List<ShiftType> cycle;
    private final List<Boolean> waterDays;

    public ShiftConfig(LocalDate startDate, List<ShiftType> cycle, List<Boolean> waterDays) {
        if (cycle == null || cycle.isEmpty()) {
            throw new IllegalArgumentException("Цикл графика не может быть пустым");
        }
        this.startDate = startDate;
        this.cycle = Collections.unmodifiableList(new ArrayList<>(cycle));
        this.waterDays = (waterDays != null) ? Collections.unmodifiableList(new ArrayList<>(waterDays)) : null;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public List<ShiftType> getCycle() {
        return cycle;
    }

    public List<Boolean> getWaterDays() {
        return waterDays;
    }

    public int getCycleLength() {
        return cycle.size();
    }
}
