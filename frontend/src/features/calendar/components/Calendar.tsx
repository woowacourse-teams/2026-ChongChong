import type { CSSProperties } from 'react';
import { tokens } from '../../../styles/global';
import { Text } from '../../../shared/ui/Typography';
import {
  calendarGridStyle,
  dayButtonStyle,
  monthButtonStyle,
  monthHeaderStyle,
  selectedDayStyle,
  weekdayRowStyle,
} from '../../../shared/ui/date-time-picker/dateTimePicker.styles';
import {
  getCalendarDays,
  isSameDay,
  WEEKDAYS,
} from '../../../shared/ui/date-time-picker/dateTimePicker.utils';

interface CalendarProps {
  selectedDate: Date;
  getEventKinds: (date: Date) => readonly ('created' | 'due')[];
  onSelectDate: (date: Date) => void;
}

const eventKindOrder = ['created', 'due'] as const;

const contentStyle = {
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
} satisfies CSSProperties;

const dateButtonStyle = { height: '40px' } satisfies CSSProperties;

const dayContentStyle = {
  position: 'relative',
  display: 'grid',
  placeItems: 'center',
  width: 36,
  height: 36,
} satisfies CSSProperties;

const selectedDateStyle = {
  ...selectedDayStyle,
  width: 36,
  height: 36,
  background: tokens.bg.brandSubtle,
  color: tokens.text.brand,
} satisfies CSSProperties;

const eventDotsStyle = {
  position: 'absolute',
  bottom: 4,
  display: 'flex',
  gap: 3,
} satisfies CSSProperties;

const eventDotStyle = {
  width: 4,
  height: 4,
  borderRadius: tokens.radius.full,
} satisfies CSSProperties;

const legendStyle = {
  display: 'flex',
  justifyContent: 'flex-start',
  gap: tokens.spacing[5],
  margin: `${tokens.spacing[3]} ${tokens.spacing[3]} 0`,
  padding: `${tokens.spacing[3]} 0`,
  borderTop: tokens.border.neutral,
} satisfies CSSProperties;

const legendItemStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
  color: tokens.text.default,
} satisfies CSSProperties;

const createdLegendDotStyle = {
  ...eventDotStyle,
  width: 7,
  height: 7,
  flexShrink: 0,
  background: tokens.bg.brand,
} satisfies CSSProperties;

const dueLegendDotStyle = {
  ...createdLegendDotStyle,
  background: tokens.text.critical,
} satisfies CSSProperties;

export default function Calendar({ selectedDate, getEventKinds, onSelectDate }: CalendarProps) {
  const year = selectedDate.getFullYear();
  const month = selectedDate.getMonth();
  const lastDateOfMonth = new Date(selectedDate);
  lastDateOfMonth.setMonth(month + 1, 0);
  const calendarDays = getCalendarDays(selectedDate).filter(
    ({ date }) => date.getTime() <= lastDateOfMonth.getTime(),
  );

  function moveMonth(offset: number) {
    const nextDate = new Date(selectedDate);
    nextDate.setMonth(month + offset, 1);
    onSelectDate(nextDate);
  }

  return (
    <div css={contentStyle}>
      <div css={monthHeaderStyle}>
        <button
          type="button"
          css={monthButtonStyle}
          aria-label="이전 달"
          disabled={year === 1 && month === 0}
          onClick={() => moveMonth(-1)}
        >
          ‹
        </button>
        <Text variant="inherited">
          {year}년 {month + 1}월
        </Text>
        <button
          type="button"
          css={monthButtonStyle}
          aria-label="다음 달"
          disabled={year === 9998 && month === 11}
          onClick={() => moveMonth(1)}
        >
          ›
        </button>
      </div>

      <div css={weekdayRowStyle} aria-hidden="true">
        {WEEKDAYS.map((weekday, index) => (
          <Text
            key={weekday}
            variant="inherited"
            css={index === 0 && { color: tokens.text.critical }}
          >
            {weekday}
          </Text>
        ))}
      </div>

      <div css={[calendarGridStyle, { gridAutoRows: '40px' }]} role="group" aria-label="날짜 선택">
        {calendarDays.map(({ date, isCurrentMonth }) => {
          if (!isCurrentMonth) return <span key={date.getTime()} aria-hidden="true" />;

          const day = date.getDate();
          const isSelected = isSameDay(date, selectedDate);
          const dayEventKinds = getEventKinds(date);
          const eventKinds = eventKindOrder.filter((kind) => dayEventKinds.includes(kind));
          return (
            <button
              key={date.getTime()}
              type="button"
              css={[dayButtonStyle, dateButtonStyle]}
              aria-label={`${year}년 ${month + 1}월 ${day}일${eventKinds.length ? ' · 일정 있음' : ''}`}
              aria-pressed={isSelected}
              onClick={() => onSelectDate(date)}
            >
              <span
                css={[
                  dayContentStyle,
                  isSelected && selectedDateStyle,
                  !isSelected && date.getDay() === 0 && { color: tokens.text.critical },
                ]}
              >
                <Text
                  variant="inherited"
                  css={eventKinds.length > 0 && { transform: 'translateY(-3px)' }}
                >
                  {day}
                </Text>
                {eventKinds.length > 0 && (
                  <span aria-hidden="true" css={eventDotsStyle}>
                    {eventKinds.map((kind) => (
                      <span
                        key={kind}
                        css={[
                          eventDotStyle,
                          {
                            background: kind === 'due' ? tokens.text.critical : tokens.bg.brand,
                          },
                        ]}
                      />
                    ))}
                  </span>
                )}
              </span>
            </button>
          );
        })}
      </div>
      <div css={legendStyle} role="group" aria-label="일정 표시 안내">
        <Text variant="footnote" css={legendItemStyle}>
          <span css={createdLegendDotStyle} aria-hidden="true" />
          공지·과제 생성
        </Text>
        <Text variant="footnote" css={legendItemStyle}>
          <span css={dueLegendDotStyle} aria-hidden="true" />
          과제 마감
        </Text>
      </div>
    </div>
  );
}
