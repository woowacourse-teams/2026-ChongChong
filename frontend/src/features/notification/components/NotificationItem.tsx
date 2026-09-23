import { CSSProperties } from 'react';
import { CSSObject } from '@emotion/react';
import reminderIcon from '../../../shared/assets/remind.svg';
import notificationIcon from '../../../shared/assets/notice-green.svg';
import { Notification } from '../types';
import { typography, tokens } from '../../../styles/global';
import { formatRelativeTime } from '../../../shared/utils/formatDate';

const NotificationItemStyle = {
  display: 'grid',
  gridTemplateColumns: '36px minmax(0, 1fr) 8px',
  columnGap: '11px',
  minHeight: '95px',
  padding: `${tokens.spacing[4]} ${tokens.spacing[4]} 7px`,
  borderRadius: tokens.radius.md,
  color: tokens.text.primary,
  backgroundColor: tokens.bg.default,
  transition: 'background-color 300ms ease',
  '&:hover': {
    backgroundColor: tokens.bg.neutral,
  } satisfies CSSProperties,
} satisfies CSSObject;

const TextStyle = {
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const DescriptionStyle = {
  ...typography.caption,
  ...TextStyle,
  maxWidth: '202px',
  marginTop: tokens.spacing[1],
  color: tokens.text.muted,
} satisfies CSSProperties;

const TimeStyle = {
  ...typography.footnote,
  display: 'block',
  marginTop: tokens.spacing[2],
  color: tokens.text.muted,
} satisfies CSSProperties;

const UnreadStyle = {
  width: '8px',
  height: '8px',
  marginTop: '5px',
  borderRadius: tokens.radius.full,
  background: tokens.bg.brand,
} satisfies CSSProperties;

// TODO: 변화에 대응할 수 있는 컴포넌트 설계하기
export default function NotificationItem({ notification }: { notification: Notification }) {
  const isReminder = notification.type === 'REMIND';
  return (
    <div css={NotificationItemStyle}>
      {isReminder ? <ReminderNotificationIcon /> : <CommonNotificationIcon />}
      <div css={{ minWidth: 0 }}>
        <p css={{ ...typography.subtitle, ...TextStyle }}>{notification.title}</p>
        <p css={DescriptionStyle}>{notification.body}</p>
        <time css={TimeStyle} dateTime={notification.createdAt}>
          {formatRelativeTime(notification.createdAt)}
        </time>
      </div>
      {!notification.isRead && <UnreadMark />}
    </div>
  );
}

function UnreadMark() {
  return <span css={UnreadStyle} role="img" aria-label="읽지 않은 알림" />;
}

function ReminderNotificationIcon() {
  return (
    <img
      src={reminderIcon}
      width={36}
      height={36}
      alt="리마인드 알림"
      css={{ justifySelf: 'center', marginTop: 0 }}
    />
  );
}

function CommonNotificationIcon() {
  return (
    <img
      src={notificationIcon}
      alt="일반 알림"
      width={18}
      height={18}
      css={{ justifySelf: 'center', marginTop: tokens.spacing[2] }}
    />
  );
}
