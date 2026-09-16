package com.shiftcalendar.app.ui;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shiftcalendar.app.R;
import com.shiftcalendar.app.logic.ShiftCalculator;
import com.shiftcalendar.app.model.DayStatus;
import com.shiftcalendar.app.model.ShiftConfig;
import com.shiftcalendar.app.model.ShiftType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

/**
 * Строит и отображает сетку календаря на месяц: по одной строке на неделю,
 * с номером недели слева и 7 ячейками дней (Пн..Вс) справа.
 */
public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.WeekViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(LocalDate date);
    }

    private final List<List<LocalDate>> weeks = new ArrayList<>();
    private ShiftConfig shiftConfig;
    private YearMonth currentMonth;
    private LocalDate selectedDate;
    private final OnDayClickListener listener;

    public CalendarAdapter(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void submit(ShiftConfig config, YearMonth month, LocalDate selected) {
        this.shiftConfig = config;
        this.currentMonth = month;
        this.selectedDate = selected;
        rebuildWeeks();
        notifyDataSetChanged();
    }

    public void setSelectedDate(LocalDate selected) {
        this.selectedDate = selected;
        notifyDataSetChanged();
    }

    private void rebuildWeeks() {
        weeks.clear();
        LocalDate firstOfMonth = currentMonth.atDay(1);
        LocalDate lastOfMonth = currentMonth.atEndOfMonth();

        // Начинаем сетку с понедельника той недели, в которую попадает 1-е число.
        LocalDate gridStart = firstOfMonth.minusDays(
                (firstOfMonth.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue() + 7) % 7);
        // Заканчиваем воскресеньем той недели, в которую попадает последнее число.
        LocalDate gridEnd = lastOfMonth.plusDays(
                (DayOfWeek.SUNDAY.getValue() - lastOfMonth.getDayOfWeek().getValue() + 7) % 7);

        LocalDate cursor = gridStart;
        while (!cursor.isAfter(gridEnd)) {
            List<LocalDate> week = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                week.add(cursor);
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }
    }

    @NonNull
    @Override
    public WeekViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_week_row, parent, false);
        return new WeekViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeekViewHolder holder, int position) {
        List<LocalDate> week = weeks.get(position);

        // Номер недели вычисляем по первому дню строки (ISO-неделя).
        int weekNumber = week.get(0).get(WeekFields.ISO.weekOfWeekBasedYear());
        holder.tvWeekNumber.setText(String.valueOf(weekNumber));

        holder.daysContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (LocalDate date : week) {
            View cell = inflater.inflate(R.layout.item_day_cell, holder.daysContainer, false);
            bindDayCell(cell, date);
            holder.daysContainer.addView(cell);
        }
    }

    private void bindDayCell(View cell, LocalDate date) {
        TextView tvNumber = cell.findViewById(R.id.tvNumber);
        View ringBg = cell.findViewById(R.id.ringBg);
        TextView tvShiftIcon = cell.findViewById(R.id.tvShiftIcon);

        tvNumber.setText(String.valueOf(date.getDayOfMonth()));

        boolean isCurrentMonth = YearMonth.from(date).equals(currentMonth);
        boolean isSunday = date.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean isSelected = date.equals(selectedDate);

        int textColor;
        if (isSelected) {
            textColor = cell.getResources().getColor(R.color.text_white);
        } else if (isSunday) {
            textColor = cell.getResources().getColor(R.color.text_red_sunday);
        } else {
            textColor = cell.getResources().getColor(R.color.text_white);
        }
        tvNumber.setTextColor(textColor);
        tvNumber.setAlpha(isCurrentMonth ? 1f : 0.35f);

        if (shiftConfig != null) {
            DayStatus status = ShiftCalculator.getStatus(shiftConfig, date);
            ShiftType type = status.getShiftType();
            boolean isWater = status.isWaterDay();

            if (type.isWork()) {
                ringBg.setVisibility(View.VISIBLE);
                GradientDrawable shape = (GradientDrawable) ringBg.getBackground();
                shape.setStroke(4, type.getColor());
            } else {
                ringBg.setVisibility(View.GONE);
            }
            
            StringBuilder iconText = new StringBuilder();
            if (type.isWork()) iconText.append(type.getIcon());
            if (isWater) iconText.append("💧");
            
            String finalIcons = iconText.toString();
            if (finalIcons.isEmpty()) {
                tvShiftIcon.setVisibility(View.GONE);
            } else {
                tvShiftIcon.setVisibility(View.VISIBLE);
                tvShiftIcon.setText(finalIcons);
            }
        } else {
            ringBg.setVisibility(View.GONE);
            tvShiftIcon.setVisibility(View.GONE);
        }

        tvNumber.setBackgroundResource(
                isSelected ? R.drawable.circle_selected_bg : R.drawable.ripple_day_cell);

        cell.setAlpha(isCurrentMonth ? 1f : 0.55f);

        tvNumber.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDayClick(date);
            }
        });
    }

    @Override
    public int getItemCount() {
        return weeks.size();
    }

    static class WeekViewHolder extends RecyclerView.ViewHolder {
        final TextView tvWeekNumber;
        final LinearLayout daysContainer;

        WeekViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWeekNumber = itemView.findViewById(R.id.tvWeekNumber);
            daysContainer = itemView.findViewById(R.id.daysContainer);
        }
    }
}
