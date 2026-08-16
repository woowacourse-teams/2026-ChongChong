import { useEffect, useEffectEvent, useRef } from 'react';
import type { UIEvent } from 'react';
import {
  summaryStyle,
  wheelColumnStyle,
  wheelOptionStyle,
  wheelPickerStyle,
  wheelSelectionStyle,
} from './dateTimePicker.styles';
import { formatTime, getTimeMinutes, TIME_INTERVAL_MINUTES } from './dateTimePicker.utils';

export interface TimePickerProps {
  value: Date;
  onChange: (value: Date) => void;
}

type WheelType = 'period' | 'hour' | 'minute';

const ITEM_HEIGHT = 44;
const PERIODS = ['오전', '오후'];
const HOURS = Array.from({ length: 12 }, (_, index) => index + 1);
const MINUTES = Array.from(
  { length: 60 / TIME_INTERVAL_MINUTES },
  (_, index) => index * TIME_INTERVAL_MINUTES,
);

function clampIndex(index: number, length: number) {
  return Math.min(Math.max(index, 0), length - 1);
}

export default function TimePicker({ value, onChange }: TimePickerProps) {
  const periodRef = useRef<HTMLDivElement>(null);
  const hourRef = useRef<HTMLDivElement>(null);
  const minuteRef = useRef<HTMLDivElement>(null);
  const scrollTimersRef = useRef<Partial<Record<WheelType, number>>>({});
  const periodIndex = value.getHours() >= 12 ? 1 : 0;
  const selectedHour = value.getHours() % 12 || 12;
  const hourIndex = selectedHour - 1;
  const minuteIndex = Math.round(value.getMinutes() / TIME_INTERVAL_MINUTES);

  const syncWheelPositions = useEffectEvent(() => {
    if (periodRef.current) periodRef.current.scrollTop = periodIndex * ITEM_HEIGHT;
    if (hourRef.current) hourRef.current.scrollTop = hourIndex * ITEM_HEIGHT;
    if (minuteRef.current) minuteRef.current.scrollTop = minuteIndex * ITEM_HEIGHT;
  });

  const clearScrollTimers = useEffectEvent(() => {
    Object.values(scrollTimersRef.current).forEach(window.clearTimeout);
  });

  useEffect(() => {
    syncWheelPositions();
  }, [hourIndex, minuteIndex, periodIndex]);

  useEffect(() => clearScrollTimers, []);

  const selectIndex = (type: WheelType, index: number) => {
    const nextValue = new Date(value);

    if (type === 'period') {
      nextValue.setHours((selectedHour % 12) + (index === 1 ? 12 : 0));
    } else if (type === 'hour') {
      const hour = HOURS[index];
      nextValue.setHours((hour % 12) + (periodIndex === 1 ? 12 : 0));
    } else {
      nextValue.setMinutes(MINUTES[index], 0, 0);
    }

    onChange(nextValue);
  };

  const handleScroll = (type: WheelType, event: UIEvent<HTMLDivElement>, itemCount: number) => {
    const element = event.currentTarget;
    window.clearTimeout(scrollTimersRef.current[type]);
    scrollTimersRef.current[type] = window.setTimeout(() => {
      const index = clampIndex(Math.round(element.scrollTop / ITEM_HEIGHT), itemCount);
      element.scrollTo({ top: index * ITEM_HEIGHT, behavior: 'smooth' });
      selectIndex(type, index);
    }, 80);
  };

  return (
    <>
      <div css={summaryStyle}>시간 · {formatTime(getTimeMinutes(value))}</div>

      <div css={wheelPickerStyle} aria-label="시간 선택">
        <div css={wheelSelectionStyle} aria-hidden="true" />

        <div
          ref={periodRef}
          css={wheelColumnStyle}
          aria-label="오전 오후"
          onScroll={(event) => handleScroll('period', event, PERIODS.length)}
        >
          {PERIODS.map((period, index) => (
            <button
              key={period}
              type="button"
              css={[wheelOptionStyle, { opacity: periodIndex === index ? 1 : 0.48 }]}
              aria-pressed={periodIndex === index}
              onClick={() => selectIndex('period', index)}
            >
              {period}
            </button>
          ))}
        </div>

        <div
          ref={hourRef}
          css={wheelColumnStyle}
          aria-label="시"
          onScroll={(event) => handleScroll('hour', event, HOURS.length)}
        >
          {HOURS.map((hour, index) => (
            <button
              key={hour}
              type="button"
              css={[wheelOptionStyle, { opacity: hourIndex === index ? 1 : 0.48 }]}
              aria-label={`${hour}시`}
              aria-pressed={hourIndex === index}
              onClick={() => selectIndex('hour', index)}
            >
              {hour}
            </button>
          ))}
        </div>

        <div
          ref={minuteRef}
          css={wheelColumnStyle}
          aria-label="분"
          onScroll={(event) => handleScroll('minute', event, MINUTES.length)}
        >
          {MINUTES.map((minute, index) => (
            <button
              key={minute}
              type="button"
              css={[wheelOptionStyle, { opacity: minuteIndex === index ? 1 : 0.48 }]}
              aria-label={`${minute}분`}
              aria-pressed={minuteIndex === index}
              onClick={() => selectIndex('minute', index)}
            >
              {String(minute).padStart(2, '0')}
            </button>
          ))}
        </div>
      </div>
    </>
  );
}
