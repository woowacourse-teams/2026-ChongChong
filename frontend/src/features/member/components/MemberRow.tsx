import { CSSProperties, ReactNode } from 'react';
import { tokens, typography } from '../../../styles/global';
import crownIcon from '../../../shared/assets/lead.svg';
import { StudyRole } from '../types';

interface Props {
  left?: React.ReactNode;
  right?: React.ReactNode;
}

interface ProfileProps {
  name: string;
  icon?: ReactNode;
}

interface LeaderProps {
  name: string;
  role: StudyRole;
  onKick: () => void;
}

interface MemberProps {
  name: string;
  role: StudyRole;
}

const rowStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: tokens.spacing[2],
  width: '100%',
  height: '68px',
  padding: `0 ${tokens.spacing[5]}`,
  background: tokens.bg.default,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
} satisfies CSSProperties;

const profileStyle = {
  display: 'flex',
  minWidth: 0,
  alignItems: 'center',
  gap: '6px',
} satisfies CSSProperties;

const avatarStyle = {
  ...typography.label,
  display: 'inline-flex',
  width: '28px',
  height: '28px',
  flex: '0 0 28px',
  alignItems: 'center',
  justifyContent: 'center',
  marginRight: tokens.spacing[2],
  background: tokens.color.cardLine8,
  borderRadius: tokens.radius.full,
  color: tokens.text.muted,
} satisfies CSSProperties;

const nameStyle = {
  ...typography.subtitle,
  overflow: 'hidden',
  color: tokens.text.primary,
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const iconStyle = {
  width: '24px',
  height: '24px',
  flex: '0 0 24px',
} satisfies CSSProperties;

const kickButtonStyle = {
  ...typography.body,
  flex: '0 0 auto',
  color: tokens.text.brand,
  cursor: 'pointer',
} satisfies CSSProperties;

export default function MemberRow({ left, right }: Props) {
  return (
    <div css={rowStyle}>
      {left}
      {right}
    </div>
  );
}

function Profile({ name, icon }: ProfileProps) {
  return (
    <div css={profileStyle}>
      <span css={avatarStyle} aria-hidden="true">
        {name.slice(0, 1)}
      </span>
      <span css={nameStyle}>{name}</span>
      {icon}
    </div>
  );
}

MemberRow.Leader = function Leader({ name, role, onKick }: LeaderProps) {
  return (
    <MemberRow
      left={
        <Profile
          name={name}
          icon={role === 'LEADER' && <img css={iconStyle} src={crownIcon} alt="스터디 리드" />}
        />
      }
      right={
        role !== 'LEADER' && (
          <button css={kickButtonStyle} type="button" onClick={onKick}>
            방출하기
          </button>
        )
      }
    />
  );
};

MemberRow.Member = function Member({ name, role }: MemberProps) {
  return (
    <MemberRow
      left={
        <Profile
          name={name}
          icon={role === 'LEADER' && <img css={iconStyle} src={crownIcon} alt="스터디 리드" />}
        />
      }
    />
  );
};
