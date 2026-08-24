import { CSSProperties } from 'react';
import assignmentLogo from '../../../shared/assets/assign-green.svg';
import noticeLogo from '../../../shared/assets/notice-green.svg';
import { tokens, typography } from '../../../styles/global';

interface ActiveStudyCardProps {
  icon: string;
  title: string;
  status: React.ReactNode;
}

interface LeaderActiveNoticeCardProps {
  title: string;
  completeCount: number;
  memberCount: number;
}

interface LeaderActiveAssignmentCardProps {
  title: string;
  completeCount: number;
  memberCount: number;
}

interface MemberActiveNoticeCardProps {
  title: string;
}

interface MemberActiveAssignmentCardProps {
  title: string;
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
      {status}
    </div>
  );
}

export function LeaderActiveNoticeCard({
  title,
  completeCount,
  memberCount,
}: LeaderActiveNoticeCardProps) {
  return (
    <ActiveStudyCard
      icon={noticeLogo}
      title={title}
      status={<span css={StatusStyle}>{`${completeCount}/${memberCount} 읽음`}</span>}
    />
  );
}

export function LeaderActiveAssignmentCard({
  title,
  completeCount,
  memberCount,
}: LeaderActiveAssignmentCardProps) {
  return (
    <ActiveStudyCard
      icon={assignmentLogo}
      title={title}
      status={<span css={StatusStyle}>{`${completeCount}/${memberCount} 제출`}</span>}
    />
  );
}

export function MemberActiveNoticeCard({ title }: MemberActiveNoticeCardProps) {
  return (
    <ActiveStudyCard
      icon={noticeLogo}
      title={title}
      status={<span css={StatusStyle}>읽지 않음</span>}
    />
  );
}

export function MemberActiveAssignmentCard({ title }: MemberActiveAssignmentCardProps) {
  return (
    <ActiveStudyCard
      icon={assignmentLogo}
      title={title}
      status={<span css={StatusStyle}>미제출</span>}
    />
  );
}
