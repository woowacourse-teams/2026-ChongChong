import { useState, type CSSProperties } from 'react';
import { getKoreanToday } from '../utils';
import { Link } from 'react-router';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import Calendar from './Calendar';
import { formatDate } from '../../../shared/ui/date-time-picker/dateTimePicker.utils';
import NoticeIcon from '../../../shared/assets/notice-green.webp';
import AssignmentIcon from '../../../shared/assets/assign-green.webp';
import { Heading, Paragraph, Text } from '../../../shared/ui/Typography';
import { tokens } from '../../../styles/global';

export interface CalendarEvent {
  id: string;
  resourceId: number;
  label: string;
  title: string;
  type: 'notice' | 'assignment' | 'due';
  time?: string;
}

interface StudyCalendarProps {
  studyId: number;
  eventsByDate: Readonly<Record<string, readonly CalendarEvent[]>>;
}

const eventListStyle = {
  margin: 0,
  padding: 0,
  listStyle: 'none',
  display: 'grid',
  gap: tokens.spacing[3],
} satisfies CSSProperties;

const cardStyle = {
  padding: tokens.spacing[3],
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
} satisfies CSSProperties;

const eventStyle = {
  ...cardStyle,
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[3],
  minHeight: 64,
  color: tokens.text.default,
  textDecoration: 'none',
} satisfies CSSProperties;

const dotStyle = {
  flexShrink: 0,
  width: 7,
  height: 7,
  borderRadius: tokens.radius.full,
} satisfies CSSProperties;

const emptyStyle = {
  ...cardStyle,
  display: 'flex',
  minHeight: 140,
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  textAlign: 'center',
} satisfies CSSProperties;

export default function StudyCalendar({ studyId, eventsByDate }: StudyCalendarProps) {
  const [selectedDate, setSelectedDate] = useState(getKoreanToday);
  const events = eventsByDate[formatDate(selectedDate)] ?? [];

  const handleDate = (date: Date) => {
    setSelectedDate(date);
  };

  return (
    <>
      <Calendar
        selectedDate={selectedDate}
        getEventKinds={(date) =>
          Array.from(
            new Set(
              eventsByDate[formatDate(date)]?.map(({ type }) =>
                type === 'due' ? 'due' : 'created',
              ),
            ),
          )
        }
        onSelectDate={handleDate}
      />
      <section aria-labelledby="study-calendar-events" css={{ marginTop: tokens.spacing[6] }}>
        <Heading
          id="study-calendar-events"
          level={2}
          variant="sectionLabel"
          css={{ margin: `0 0 ${tokens.spacing[3]}` }}
        >
          해당 날짜 일정
        </Heading>
        {events.length ? (
          <ul css={eventListStyle}>
            {events.map(({ id, resourceId, label, title, type, time }) => {
              const color = type === 'due' ? tokens.text.critical : tokens.text.brand;

              return (
                <li key={id}>
                  <Link
                    css={eventStyle}
                    {...(type === 'notice'
                      ? URL_POLICY.toNotice(studyId, resourceId)
                      : URL_POLICY.toAssignment(studyId, resourceId))}
                  >
                    <span aria-hidden="true" css={[dotStyle, { backgroundColor: color }]} />
                    <img
                      src={type === 'notice' ? NoticeIcon : AssignmentIcon}
                      width={18}
                      height={18}
                      alt=""
                    />
                    <Text
                      variant="footnote"
                      css={{ flex: 1, minWidth: 0, overflowWrap: 'anywhere' }}
                    >
                      {title}
                    </Text>
                    <Text variant="footnote" css={{ color, flexShrink: 0 }}>
                      {time ?? label}
                    </Text>
                  </Link>
                </li>
              );
            })}
          </ul>
        ) : (
          <div css={emptyStyle}>
            <Paragraph variant="body" css={{ color: tokens.text.muted }}>
              등록된 일정이 없어요
            </Paragraph>
            <Paragraph variant="footnote" css={{ color: tokens.text.muted }}>
              다른 날짜의 일정을 확인해 보세요.
            </Paragraph>
          </div>
        )}
      </section>
    </>
  );
}
