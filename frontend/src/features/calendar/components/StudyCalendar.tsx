import { useState, type CSSProperties } from 'react';
import { getKoreanToday } from '../utils';
import { useSuspenseQuery } from '@tanstack/react-query';
import calendarQueries from '../queries';
import type { CalendarEvent } from '../responseSchemas';
import Calendar from './Calendar';
import CalendarEventItem from './CalendarEventItem';
import { formatDate } from '../../../shared/ui/date-time-picker/dateTimePicker.utils';
import List from '../../../shared/ui/List';
import { Heading, Paragraph } from '../../../shared/ui/Typography';
import { tokens } from '../../../styles/global';

interface StudyCalendarProps {
  studyId: number;
}

const cardStyle = {
  padding: tokens.spacing[3],
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
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

export default function StudyCalendar({ studyId }: StudyCalendarProps) {
  const [selectedDate, setSelectedDate] = useState(getKoreanToday);
  const { data } = useSuspenseQuery(
    calendarQueries.month(studyId, selectedDate.getFullYear(), selectedDate.getMonth() + 1),
  );

  const eventsByDate: Record<string, CalendarEvent[]> = {};
  for (const event of data.events) {
    const day = event.eventAt.slice(0, 10).replaceAll('-', '.');
    (eventsByDate[day] ??= []).push(event);
  }
  const events = eventsByDate[formatDate(selectedDate).padStart(10, '0')] ?? [];

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
              eventsByDate[formatDate(date).padStart(10, '0')]?.map(({ eventType }) =>
                eventType === 'ASSIGNMENT_DUE' ? 'due' : 'created',
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
          <List>
            {events.map((event) => (
              <CalendarEventItem
                key={`${event.eventType}-${event.resourceId}`}
                studyId={studyId}
                event={event}
              />
            ))}
          </List>
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
