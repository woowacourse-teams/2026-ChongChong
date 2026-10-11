import type { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';
import { AssignmentSubmitStatus } from '../types';
import { PostHogCaptureOnViewed } from '@posthog/react';

interface Props {
  status: AssignmentSubmitStatus;
}

const cardStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
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

export default function SubmitStatus({ status }: Props) {
  const progress = status.memberCount === 0 ? 0 : (status.completeCount / status.memberCount) * 100;

  return (
    <PostHogCaptureOnViewed name="assignment-submit-status-section">
      <section css={cardStyle} aria-labelledby="submit-status-title">
        <header css={headerStyle}>
          <h2 id="submit-status-title" css={statusTitleStyle}>
            제출 현황
          </h2>
        </header>

        <div css={countStyle}>
          <strong css={readCountStyle}>{status.completeCount}</strong>
          <span css={totalCountStyle}>/ {status.memberCount}명</span>
        </div>

        <div
          css={progressTrackStyle}
          role="progressbar"
          aria-label="과제 제출률"
          aria-valuemin={0}
          aria-valuemax={status.memberCount}
          aria-valuenow={status.completeCount}
        >
          <div css={{ ...progressBarStyle, width: `${progress}%` }} />
        </div>
      </section>
    </PostHogCaptureOnViewed>
  );
}
