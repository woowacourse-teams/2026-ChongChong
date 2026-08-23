import { CSSProperties } from 'react';
import assignmentLogo from '../../../shared/assets/assign-green.svg';
import noticeLogo from '../../../shared/assets/notice-green.svg';
import { tokens, typography } from '../../../styles/global';

interface ActiveStudyCardProps {
  icon: string;
  title: string;
  status: string;
}

interface ActiveNoticeCardProps {
  title: string;
  completeCount: number;
  memberCount: number;
}

interface ActiveAssignmentCardProps {
  title: string;
  completeCount: number;
  memberCount: number;
}

const ActiveStudyCardStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[3],
  padding: `${tokens.spacing[6]} ${tokens.spacing[5]}`,
  background: tokens.bg.default,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
  boxShadow: tokens.shadow[1],
} satisfies CSSProperties;

const IconStyle = {
  width: '18px',
  height: '18px',
} satisfies CSSProperties;

const TitleStyle = {
  ...typography.body,
  flex: 1,
  margin: 0,
  color: tokens.text.primary,
  overflow: 'hidden',
  whiteSpace: 'nowrap',
  textOverflow: 'ellipsis',
} satisfies CSSProperties;

const StatusStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

function ActiveStudyCard({ icon, title, status }: ActiveStudyCardProps) {
  return (
    <div css={ActiveStudyCardStyle}>
      <img css={IconStyle} src={icon} alt="" />
      <h3 css={TitleStyle}>{title}</h3>
      <span css={StatusStyle}>{status}</span>
    </div>
  );
}

export function ActiveNoticeCard({ title, completeCount, memberCount }: ActiveNoticeCardProps) {
  return (
    <ActiveStudyCard
      icon={noticeLogo}
      title={title}
      status={`${completeCount}/${memberCount} 읽음`}
    />
  );
}

export function ActiveAssignmentCard({
  title,
  completeCount,
  memberCount,
}: ActiveAssignmentCardProps) {
  return (
    <ActiveStudyCard
      icon={assignmentLogo}
      title={title}
      status={`${completeCount}/${memberCount} 제출`}
    />
  );
}
