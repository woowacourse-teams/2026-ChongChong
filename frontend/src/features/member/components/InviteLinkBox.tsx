import { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';
import CopyIcon from '../../../shared/assets/copy.svg';
import { usePostHog } from '@posthog/react';

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

export default function InviteLinkBox({
  title,
  inviteLink,
}: {
  title: string;
  inviteLink: string;
}) {
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
