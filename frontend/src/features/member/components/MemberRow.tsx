import { CSSProperties, ReactNode } from 'react';
import { tokens, typography } from '../../../styles/global';
import crownIcon from '../../../shared/assets/lead.svg';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import useDialogControl from '../../../shared/hooks/useDialogControl';
import { StudyRole } from '../types';

interface Props extends React.ComponentProps<'div'> {
  left?: React.ReactNode;
  right?: React.ReactNode;
}

interface ProfileProps {
  name: string;
  icon?: ReactNode;
}

interface LeaderProps extends React.ComponentProps<'div'> {
  name: string;
  role: StudyRole;
  onKick: () => void;
}

interface MemberProps extends React.ComponentProps<'div'> {
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

export default function MemberRow({ left, right, ...props }: Props) {
  return (
    <div css={rowStyle} {...props}>
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

MemberRow.Leader = function Leader({ name, role, onKick, ...props }: LeaderProps) {
  const { dialogRef, open, close } = useDialogControl();

  return (
    <MemberRow
      {...props}
      left={
        <Profile
          name={name}
          icon={role === 'LEADER' && <img css={iconStyle} src={crownIcon} alt="스터디 리드" />}
        />
      }
      right={
        role !== 'LEADER' && (
          <>
            <button css={kickButtonStyle} type="button" onClick={open}>
              방출하기
            </button>
            <ConfirmDialog
              ref={dialogRef}
              title={`${name} 님을 추방하시겠습니까?`}
              description={
                '추방된 스터디원은 스터디 정보에 다시 접근할 수 없으며, 이 작업은 되돌릴 수 없습니다.'
              }
              closeButton={
                <ConfirmDialog.CloseButton onClick={close}>취소</ConfirmDialog.CloseButton>
              }
              confirmButton={
                <ConfirmDialog.ConfirmButton onClick={onKick}>추방</ConfirmDialog.ConfirmButton>
              }
            />
          </>
        )
      }
    />
  );
};

MemberRow.Member = function Member({ name, role, ...props }: MemberProps) {
  return (
    <MemberRow
      {...props}
      left={
        <Profile
          name={name}
          icon={role === 'LEADER' && <img css={iconStyle} src={crownIcon} alt="스터디 리드" />}
        />
      }
    />
  );
};
