import { CSSProperties } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { usePostHog } from '@posthog/react';
import { tokens, typography } from '../../../styles/global';
import CopyIcon from '../../../shared/assets/copy.svg';
import CopySuccessIcon from '../../../shared/assets/check.svg';
import studyQueries from '../../study/queries';
import useCopyLink from '../hooks/useCopyLink';

interface InviteStudyLinkSectionProps {
  studyId: number;
}

interface InviteLinkBoxProps {
  inviteLink: string;
}

interface InviteStudyLinkSectionFallbackProps {
  message?: string;
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

export function InviteStudyLinkSection({ studyId }: InviteStudyLinkSectionProps) {
  const {
    data: { inviteLink },
  } = useSuspenseQuery(studyQueries.inviteLink(studyId));

  return (
    <section>
      <h2 css={typography.subtitle}>초대 링크</h2>
      <InviteLinkBox inviteLink={inviteLink} />
      <p css={inviteDescriptionStyle}>링크를 통해 새로운 스터디원을 초대해요</p>
    </section>
  );
}

export function InviteLinkBox({ inviteLink }: InviteLinkBoxProps) {
  const { isCopySuccess, copyLink } = useCopyLink();
  const posthog = usePostHog();

  const handleCopy = () => {
    posthog?.capture('copy-invite-link', {
      location: 'member_list_page',
    });
    copyLink(inviteLink);
  };

  return (
    <div css={inviteLinkBlockStyle}>
      <span css={inviteLinkStyle}>{inviteLink}</span>
      <button
        css={[copyButtonStyle, isCopySuccess && { cursor: 'default' }]}
        type="button"
        onClick={handleCopy}
        aria-label="링크 복사"
        disabled={isCopySuccess}
      >
        <img src={isCopySuccess ? CopySuccessIcon : CopyIcon} width={20} height={20} alt="" />
      </button>
    </div>
  );
}

export function InviteStudyLinkSectionFallback({ message }: InviteStudyLinkSectionFallbackProps) {
  return (
    <section>
      <h2 css={typography.subtitle}>초대 링크</h2>
      <div css={inviteLinkBlockStyle}>
        <span css={inviteLinkStyle} role="alert">
          {message ?? '초대링크를 가져오지 못했어요'}
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
    </section>
  );
}
