import type { CSSProperties } from 'react';
import checkIcon from '../../../shared/assets/check.svg';
import clockIcon from '../../../shared/assets/clock.svg';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import Badge from '../../../shared/ui/Badge';
import { tokens, typography } from '../../../styles/global';
import { AssignmentSubmitStatus } from '../types';

interface Props {
  status: AssignmentSubmitStatus;
}

const cardStyle = {
  display: 'flex',
  flexDirection: 'column',
  padding: tokens.spacing[5],
  border: tokens.border.neutral,
  borderRadius: tokens.radius.lg,
  background: tokens.bg.default,
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

const groupLabelStyle = {
  ...typography.footnote,
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[1],
  margin: 0,
} satisfies CSSProperties;

const groupStyle = {
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

export default function SubmitStatusSection({ status }: Props) {
  const progress = status.memberCount === 0 ? 0 : (status.completeCount / status.memberCount) * 100;

  return (
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

      <div css={groupStyle}>
        <p css={{ ...groupLabelStyle, color: tokens.text.brand }}>
          <img src={checkIcon} alt="" width={18} height={18} />
          제출 {status.completeCount}명
        </p>
        <div css={badgeRowStyle}>
          {status.completeMembers.map((member) => (
            <Badge key={member.id} variant="neutralSolid" size="large">
              <img src={profileIcon} alt="" css={profileStyle} />
              {member.name}
            </Badge>
          ))}
        </div>
      </div>

      <div css={groupStyle}>
        <p css={{ ...groupLabelStyle, color: tokens.text.muted }}>
          <img src={clockIcon} alt="" width={18} height={18} />
          미제출 {status.incompleteCount}명
        </p>
        <div css={badgeRowStyle}>
          {status.incompleteMembers.map((member) => (
            <Badge key={member.id} variant="neutralSolid" size="large">
              <img src={profileIcon} alt="" css={profileStyle} />
              {member.name}
            </Badge>
          ))}
        </div>
      </div>
    </section>
  );
}
