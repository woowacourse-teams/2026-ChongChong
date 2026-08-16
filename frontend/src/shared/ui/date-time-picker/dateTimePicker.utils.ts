export const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];
export const TIME_INTERVAL_MINUTES = 5;
export const TIME_OPTION_COUNT = (24 * 60) / TIME_INTERVAL_MINUTES;

export interface CalendarDay {
  date: Date;
  isCurrentMonth: boolean;
  isDisabled: boolean;
}

export function startOfDay(value: Date) {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}

export function isSameDay(a: Date, b: Date) {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

export function normalizeTime(value: Date) {
  const normalized = new Date(value);
  const totalMinutes = normalized.getHours() * 60 + normalized.getMinutes();
  const roundedMinutes = Math.round(totalMinutes / TIME_INTERVAL_MINUTES) * TIME_INTERVAL_MINUTES;

  normalized.setHours(0, roundedMinutes, 0, 0);
  return normalized;
}

export function formatDate(value: Date) {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const date = String(value.getDate()).padStart(2, '0');

  return `${year}.${month}.${date}`;
}

export function formatTime(totalMinutes: number) {
  const hours = Math.floor(totalMinutes / 60) % 24;
  const minutes = totalMinutes % 60;
  const period = hours < 12 ? '오전' : '오후';
  const displayHours = hours % 12 || 12;

  return `${period} ${displayHours}:${String(minutes).padStart(2, '0')}`;
}

export function getTimeMinutes(value: Date) {
  return value.getHours() * 60 + value.getMinutes();
}

export function getCalendarDays(viewDate: Date, minDate: Date): CalendarDay[] {
  const firstDay = new Date(viewDate.getFullYear(), viewDate.getMonth(), 1);
  const gridStart = new Date(firstDay);
  gridStart.setDate(firstDay.getDate() - firstDay.getDay());

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(gridStart);
    date.setDate(gridStart.getDate() + index);

    return {
      date,
      isCurrentMonth: date.getMonth() === viewDate.getMonth(),
      isDisabled: startOfDay(date).getTime() < startOfDay(minDate).getTime(),
    };
  });
}
