import { useEffect, useEffectEvent, useRef } from 'react';
import type { UIEvent } from 'react';
import {
  summaryStyle,
  wheelColumnStyle,
  wheelOptionStyle,
  wheelPickerStyle,
  wheelSelectionStyle,
} from './dateTimePicker.styles';
import {
  clampToMinimumTime,
  formatTime,
  getTimeMinutes,
  TIME_INTERVAL_MINUTES,
} from './dateTimePicker.utils';

export interface TimePickerProps {
  value: Date;
  minDate: Date;
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

export default function TimePicker({ value, minDate, onChange }: TimePickerProps) {
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

  const getValueForIndex = (type: WheelType, index: number) => {
    const nextValue = new Date(value);

    if (type === 'period') {
      nextValue.setHours((selectedHour % 12) + (index === 1 ? 12 : 0));
    } else if (type === 'hour') {
      const hour = HOURS[index];
      nextValue.setHours((hour % 12) + (periodIndex === 1 ? 12 : 0));
    } else {
      nextValue.setMinutes(MINUTES[index], 0, 0);
    }

    return nextValue;
  };

  const selectIndex = (type: WheelType, index: number) => {
    onChange(clampToMinimumTime(getValueForIndex(type, index), minDate));
  };

  const isOptionDisabled = (type: WheelType, index: number) => {
    return getValueForIndex(type, index).getTime() < minDate.getTime();
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
          {PERIODS.map((period, index) => {
            const isDisabled = isOptionDisabled('period', index);

            return (
              <button
                key={period}
                type="button"
                css={[
                  wheelOptionStyle,
                  { opacity: isDisabled ? 0.24 : periodIndex === index ? 1 : 0.48 },
                ]}
                aria-pressed={periodIndex === index}
                disabled={isDisabled}
                onClick={() => selectIndex('period', index)}
              >
                {period}
              </button>
            );
          })}
        </div>

        <div
          ref={hourRef}
          css={wheelColumnStyle}
          aria-label="시"
          onScroll={(event) => handleScroll('hour', event, HOURS.length)}
        >
          {HOURS.map((hour, index) => {
            const isDisabled = isOptionDisabled('hour', index);

            return (
              <button
                key={hour}
                type="button"
                css={[
                  wheelOptionStyle,
                  { opacity: isDisabled ? 0.24 : hourIndex === index ? 1 : 0.48 },
                ]}
                aria-label={`${hour}시`}
                aria-pressed={hourIndex === index}
                disabled={isDisabled}
                onClick={() => selectIndex('hour', index)}
              >
                {hour}
              </button>
            );
          })}
        </div>

        <div
          ref={minuteRef}
          css={wheelColumnStyle}
          aria-label="분"
          onScroll={(event) => handleScroll('minute', event, MINUTES.length)}
        >
          {MINUTES.map((minute, index) => {
            const isDisabled = isOptionDisabled('minute', index);

            return (
              <button
                key={minute}
                type="button"
                css={[
                  wheelOptionStyle,
                  { opacity: isDisabled ? 0.24 : minuteIndex === index ? 1 : 0.48 },
                ]}
                aria-label={`${minute}분`}
                aria-pressed={minuteIndex === index}
                disabled={isDisabled}
                onClick={() => selectIndex('minute', index)}
              >
                {String(minute).padStart(2, '0')}
              </button>
            );
          })}
        </div>
      </div>
    </>
  );
}
