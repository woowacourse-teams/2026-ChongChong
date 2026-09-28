import type { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';
import type { NoticeReadStatus as NoticeReadStatusData } from '../types';

interface Props {
  status: NoticeReadStatusData;
}

const cardStyle = {
  display: 'flex',
  flexDirection: 'column',
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

export default function NoticeReadStatus({ status }: Props) {
  const progress = status.memberCount === 0 ? 0 : (status.readCount / status.memberCount) * 100;

  return (
    <section css={cardStyle} aria-labelledby="read-status-title">
      <header css={headerStyle}>
        <h2 id="read-status-title" css={statusTitleStyle}>
          확인 현황
        </h2>
      </header>

      <div css={countStyle}>
        <strong css={readCountStyle}>{status.readCount}</strong>
        <span css={totalCountStyle}>/ {status.memberCount}명</span>
      </div>

      <div
        css={progressTrackStyle}
        role="progressbar"
        aria-label="공지 읽음률"
        aria-valuemin={0}
        aria-valuemax={status.memberCount}
        aria-valuenow={status.readCount}
      >
        <div css={{ ...progressBarStyle, width: `${progress}%` }} />
      </div>
    </section>
  );
}
