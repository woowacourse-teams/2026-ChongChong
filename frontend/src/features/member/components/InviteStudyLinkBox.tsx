import { CSSProperties } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { usePostHog } from '@posthog/react';
import { tokens, typography } from '../../../styles/global';
import CopyIcon from '../../../shared/assets/copy.svg';
import studyQueries from '../../study/queries';

interface InviteStudyLinkBoxProps {
  studyId: number;
}

interface InviteLinkBoxProps {
  title: string;
  inviteLink: string;
}

interface InviteStudyLinkBoxFallbackProps {
  message: string;
}

const inviteDescriptionStyle = {
  ...typography.paragraph,
  margin: `${tokens.spacing[2]} 0`,
  color: tokens.text.muted,
} satisfies CSSProperties;

const inviteLinkBlockStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  padding: tokens.spacing[4],
  background: tokens.bg.subtle,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
} satisfies CSSProperties;

const inviteLinkStyle = {
  ...typography.paragraph,
  overflow: 'hidden',
  color: tokens.text.secondary,
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const copyButtonStyle = {
  flex: '0 0 auto',
  background: 'none',
  border: 'none',
  cursor: 'pointer',
} satisfies CSSProperties;

export default function InviteStudyLinkBox({ studyId }: InviteStudyLinkBoxProps) {
  const {
    data: { inviteLink },
  } = useSuspenseQuery(studyQueries.inviteLink(studyId));

  return <InviteLinkBox title="링크를 통해 새로운 스터디원을 초대해요" inviteLink={inviteLink} />;
}

export function InviteLinkBox({ title, inviteLink }: InviteLinkBoxProps) {
  const posthog = usePostHog();

  const handleCopy = () => {
    posthog?.capture('copy-invite-link', {
      location: 'member_list_page',
    });

    navigator.clipboard.writeText(inviteLink);
  };

  return (
    <>
      <p css={inviteDescriptionStyle}>{title}</p>
      <div css={inviteLinkBlockStyle}>
        <span css={inviteLinkStyle}>{inviteLink}</span>
        <button css={copyButtonStyle} type="button" onClick={handleCopy} aria-label="링크 복사">
          <img src={CopyIcon} width={16} height={20} alt="" />
        </button>
      </div>
    </>
  );
}

export function InviteStudyLinkBoxFallback({ message }: InviteStudyLinkBoxFallbackProps) {
  return (
    <div css={inviteLinkBlockStyle}>
      <span css={inviteLinkStyle} role="alert">
        {message}
      </span>
      <button
        css={[copyButtonStyle, { cursor: 'none', opacity: 0.5 }]}
        type="button"
        aria-label="링크 복사"
        disabled
      >
        <img src={CopyIcon} width={16} height={20} alt="" />
      </button>
    </div>
  );
}
