package com.shiftcalendar.app.model;

import java.util.Objects;

/**
 * Тип смены (например, "Дневная", "Ночная", "Выходной") с иконкой и цветом.
 */
public class ShiftType {
    private final String id;
    private final String name;
    private final String icon; // Эмодзи или текст
    private final int color;   // Цвет текста или обводки
    private final boolean isWork;
    private final int startHour;
    private final int endHour;

    public ShiftType(String id, String name, String icon, int color, boolean isWork) {
        this(id, name, icon, color, isWork, 8, 20);
    }

    public ShiftType(String id, String name, String icon, int color, boolean isWork, int startHour, int endHour) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.isWork = isWork;
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public int getStartHour() { return startHour; }
    public int getEndHour() { return endHour; }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }

    public boolean isWork() {
        return isWork;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShiftType shiftType = (ShiftType) o;
        return Objects.equals(id, shiftType.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
