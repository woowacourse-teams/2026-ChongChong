import { useState } from 'react';
import { tokens } from '../../../styles/global';
import {
  calendarGridStyle,
  dayButtonStyle,
  monthButtonStyle,
  monthHeaderStyle,
  selectedDayStyle,
  summaryStyle,
  weekdayRowStyle,
} from './dateTimePicker.styles';
import {
  clampToMinimumTime,
  formatDate,
  getCalendarDays,
  isSameDay,
  startOfDay,
  WEEKDAYS,
} from './dateTimePicker.utils';

export interface DatePickerProps {
  value: Date;
  minDate?: Date;
  onChange: (value: Date) => void;
}

interface CalendarView {
  selectedMonthKey: number;
  date: Date;
}

export default function DatePicker({ value, minDate = new Date(), onChange }: DatePickerProps) {
  const selectedYear = value.getFullYear();
  const selectedMonth = value.getMonth();
  const selectedMonthKey = selectedYear * 12 + selectedMonth;
  const [calendarView, setCalendarView] = useState<CalendarView>(() => ({
    selectedMonthKey,
    date: new Date(selectedYear, selectedMonth, 1),
  }));
  const viewDate =
    calendarView.selectedMonthKey === selectedMonthKey
      ? calendarView.date
      : new Date(selectedYear, selectedMonth, 1);

  if (calendarView.selectedMonthKey !== selectedMonthKey) {
    setCalendarView({ selectedMonthKey, date: viewDate });
  }

  const minimumDate = startOfDay(minDate);
  const minimumMonth = new Date(minimumDate.getFullYear(), minimumDate.getMonth(), 1);
  const calendarDays = getCalendarDays(viewDate, minimumDate);
  const isPreviousMonthDisabled = viewDate.getTime() <= minimumMonth.getTime();

  const selectDate = (date: Date) => {
    const nextValue = new Date(value);
    nextValue.setFullYear(date.getFullYear(), date.getMonth(), date.getDate());
    onChange(clampToMinimumTime(nextValue, minDate));
  };

  return (
    <>
      <div css={summaryStyle}>날짜 · {formatDate(value)}</div>

      <div css={monthHeaderStyle}>
        <button
          type="button"
          css={monthButtonStyle}
          aria-label="이전 달"
          disabled={isPreviousMonthDisabled}
          onClick={() =>
            setCalendarView((current) => ({
              ...current,
              date: new Date(current.date.getFullYear(), current.date.getMonth() - 1, 1),
            }))
          }
        >
          ‹
        </button>
        <span>
          {viewDate.getFullYear()}년 {viewDate.getMonth() + 1}월
        </span>
        <button
          type="button"
          css={monthButtonStyle}
          aria-label="다음 달"
          onClick={() =>
            setCalendarView((current) => ({
              ...current,
              date: new Date(current.date.getFullYear(), current.date.getMonth() + 1, 1),
            }))
          }
        >
          ›
        </button>
      </div>

      <div css={weekdayRowStyle} aria-hidden="true">
        {WEEKDAYS.map((weekday, index) => (
          <span key={weekday} css={index === 0 ? { color: tokens.text.critical } : {}}>
            {weekday}
          </span>
        ))}
      </div>

      <div css={calendarGridStyle} aria-label="날짜 선택">
        {calendarDays.map(({ date, isCurrentMonth, isDisabled }) => {
          const isSelected = isSameDay(date, value);
          const isSunday = date.getDay() === 0;

          return (
            <button
              key={`${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`}
              type="button"
              css={dayButtonStyle}
              aria-label={`${date.getFullYear()}년 ${date.getMonth() + 1}월 ${date.getDate()}일`}
              aria-pressed={isSelected}
              disabled={!isCurrentMonth || isDisabled}
              onClick={() => selectDate(date)}
            >
              {isCurrentMonth ? (
                <span
                  css={
                    isSelected
                      ? selectedDayStyle
                      : {
                          color: isSunday ? tokens.text.critical : tokens.color.mainBlack,
                          opacity: isDisabled ? 0.28 : 1,
                        }
                  }
                >
                  {date.getDate()}
                </span>
              ) : null}
            </button>
          );
        })}
      </div>
    </>
  );
}
