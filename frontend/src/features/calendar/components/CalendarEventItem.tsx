import type { CSSProperties } from 'react';
import { Link } from 'react-router';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import NoticeIcon from '../../../shared/assets/notice-green.webp';
import AssignmentIcon from '../../../shared/assets/assign-green.webp';
import List from '../../../shared/ui/List';
import { Text } from '../../../shared/ui/Typography';
import { tokens } from '../../../styles/global';
import type { CalendarEvent } from '../responseSchemas';

interface CalendarEventItemProps {
  studyId: number;
  event: CalendarEvent;
}

const eventStyle = {
  padding: tokens.spacing[3],
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
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

const titleStyle = {
  flex: 1,
  minWidth: 0,
  overflowWrap: 'anywhere',
} satisfies CSSProperties;

const labelStyle = { flexShrink: 0 } satisfies CSSProperties;

export default function CalendarEventItem({ studyId, event }: CalendarEventItemProps) {
  const { eventType, resourceType, resourceId, title, eventAt } = event;
  const isNotice = resourceType === 'NOTICE';
  const isDue = eventType === 'ASSIGNMENT_DUE';
  const color = isDue ? tokens.text.critical : tokens.text.brand;
  const label = isNotice ? '공지' : '과제 등록';
  const detailLink = isNotice
    ? URL_POLICY.toNotice(studyId, resourceId)
    : URL_POLICY.toAssignment(studyId, resourceId);

  return (
    <List.Item>
      <Link css={eventStyle} {...detailLink}>
        <span aria-hidden="true" css={[dotStyle, { backgroundColor: color }]} />
        <img src={isNotice ? NoticeIcon : AssignmentIcon} width={18} height={18} alt="" />
        <Text variant="footnote" css={titleStyle}>
          {isDue ? `${title} 제출 마감` : title}
        </Text>
        <Text variant="footnote" css={[labelStyle, { color }]}>
          {isDue ? eventAt.slice(11, 16) : label}
        </Text>
      </Link>
    </List.Item>
  );
}
