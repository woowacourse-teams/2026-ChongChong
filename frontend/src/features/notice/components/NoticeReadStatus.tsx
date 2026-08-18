import type { CSSObject } from '@emotion/react';
import type { CSSProperties } from 'react';
import checkIcon from '../../../shared/assets/check.svg';
import clockIcon from '../../../shared/assets/clock.svg';
import sendIcon from '../../../shared/assets/send.svg';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import Badge from '../../../shared/ui/Badge';
import Button from '../../../shared/ui/Button';
import List from '../../../shared/ui/List';
import { tokens, typography } from '../../../styles/global';

export interface NoticeMemberStatus {
  id: number;
  name: string;
  remindedAt: string;
}

//TODO: API 연동 후 optional 제거
interface NoticeReadStatusProps {
  readMemberNames: string[];
  unreadMembers: NoticeMemberStatus[];
  totalCount: number;
  reminderText: string;
  onSendReminder?: (memberId: number) => void;
  onSendAllReminders?: () => void;
}

const cardStyle = {
  display: 'flex',
  flexDirection: 'column',
  padding: tokens.spacing[5],
  border: tokens.border.neutral,
  borderRadius: tokens.radius.lg,
  background: tokens.bg.default,
} satisfies CSSProperties;

const headerStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: tokens.spacing[2],
} satisfies CSSProperties;

const statusTitleStyle = {
  ...typography.subtitle,
  margin: 0,
  color: tokens.text.primary,
} satisfies CSSProperties;

const reminderTextStyle = {
  ...typography.footnote,
  margin: 0,
  color: tokens.text.muted,
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const countStyle = {
  display: 'flex',
  height: '36px',
  alignItems: 'baseline',
  marginTop: tokens.spacing[1],
} satisfies CSSProperties;

const readCountStyle = {
  color: tokens.text.brand,
  fontSize: '34px',
  fontWeight: 700,
  lineHeight: '36px',
} satisfies CSSProperties;

const totalCountStyle = {
  ...typography.subtitle,
  color: tokens.text.muted,
} satisfies CSSProperties;

const progressTrackStyle = {
  width: '100%',
  height: '8px',
  marginTop: tokens.spacing[1],
  overflow: 'hidden',
  borderRadius: tokens.radius.full,
  background: tokens.bg.neutral,
} satisfies CSSProperties;

const progressBarStyle = {
  height: '100%',
  borderRadius: tokens.radius.full,
  background: tokens.bg.brand,
} satisfies CSSProperties;

const groupLabelStyle = {
  ...typography.footnote,
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[1],
  margin: 0,
} satisfies CSSProperties;

const readGroupStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
  marginTop: tokens.spacing[3],
} satisfies CSSProperties;

const badgeRowStyle = {
  display: 'flex',
  flexWrap: 'wrap',
  gap: tokens.spacing[1],
} satisfies CSSProperties;

const profileStyle = {
  width: '22px',
  height: '22px',
} satisfies CSSProperties;

const unreadGroupStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
  marginTop: tokens.spacing[3],
} satisfies CSSProperties;

const memberListStyle = {
  '& > ul': {
    flex: 'none',
    gap: tokens.spacing[1],
  },
} satisfies CSSObject;

const memberStyle = {
  display: 'flex',
  minHeight: '40px',
  alignItems: 'center',
} satisfies CSSProperties;

const memberTextStyle = {
  display: 'flex',
  minWidth: 0,
  marginLeft: tokens.spacing[3],
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

const memberNameStyle = {
  ...typography.body,
  color: tokens.text.primary,
} satisfies CSSProperties;

const sentAtStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

const sendButtonStyle = {
  ...typography.paragraph,
  display: 'inline-flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
  padding: tokens.spacing[2],
  border: 0,
  background: 'transparent',
  color: tokens.text.brand,
  cursor: 'pointer',
} satisfies CSSProperties;

const sendAllStyle = {
  marginTop: tokens.spacing[3],
  '& > button': {
    height: '44px',
    minHeight: '44px',
  },
} satisfies CSSObject;

export default function NoticeReadStatus({
  readMemberNames,
  unreadMembers,
  totalCount,
  reminderText,
  onSendReminder,
  onSendAllReminders,
}: NoticeReadStatusProps) {
  const readCount = readMemberNames.length;
  const progress = totalCount === 0 ? 0 : (readCount / totalCount) * 100;

  return (
    <section css={cardStyle} aria-labelledby="read-status-title">
      <header css={headerStyle}>
        <h2 id="read-status-title" css={statusTitleStyle}>
          확인 현황
        </h2>
        <p css={reminderTextStyle}>{reminderText}</p>
      </header>

      <div css={countStyle}>
        <strong css={readCountStyle}>{readCount}</strong>
        <span css={totalCountStyle}>/ {totalCount}명</span>
      </div>

      <div
        css={progressTrackStyle}
        role="progressbar"
        aria-label="공지 읽음률"
        aria-valuemin={0}
        aria-valuemax={totalCount}
        aria-valuenow={readCount}
      >
        <div css={{ ...progressBarStyle, width: `${progress}%` }} />
      </div>

      <div css={readGroupStyle}>
        <p css={{ ...groupLabelStyle, color: tokens.text.brand }}>
          <img src={checkIcon} alt="읽음" width={18} height={18} />
          확인 {readCount}명
        </p>
        <div css={badgeRowStyle}>
          {readMemberNames.map((name) => (
            <Badge key={name} variant="NeutralSolid" size="Large">
              <img src={profileIcon} alt={`${name} 프로필`} css={profileStyle} />
              {name}
            </Badge>
          ))}
        </div>
      </div>

      <div css={unreadGroupStyle}>
        <p css={{ ...groupLabelStyle, color: tokens.text.muted }}>
          <img src={clockIcon} alt="미확인" width={18} height={18} />
          미확인 {unreadMembers.length}명
        </p>

        <div css={memberListStyle}>
          <List>
            {unreadMembers.map((member) => (
              <List.Item key={member.id} css={memberStyle}>
                <img src={profileIcon} alt={`${member.name} 프로필`} width={28} height={28} />
                <span css={memberTextStyle}>
                  <span css={memberNameStyle}>{member.name}</span>
                  <span css={sentAtStyle}>{member.remindedAt}</span>
                </span>
                <button
                  type="button"
                  css={sendButtonStyle}
                  onClick={() => onSendReminder?.(member.id)}
                >
                  <img src={sendIcon} alt="리마인드 보내기" width={16} height={16} />
                  보내기
                </button>
              </List.Item>
            ))}
          </List>
        </div>
      </div>

      <div css={sendAllStyle}>
        <Button variant="brandSolid" size="large" onClick={onSendAllReminders}>
          모두에게 보내기
        </Button>
      </div>
    </section>
  );
}
