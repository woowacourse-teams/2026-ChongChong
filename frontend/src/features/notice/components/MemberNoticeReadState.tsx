import type { CSSProperties } from 'react';
import { useEffect, useState } from 'react';
import checkIcon from '../../../shared/assets/check.svg';
import circleCheckIcon from '../../../shared/assets/circle-check.svg';
import { tokens, typography } from '../../../styles/global';
import { PostHogCaptureOnViewed } from '@posthog/react';
interface MemberNoticeReadStateProps {
  progress: number;
  isRead: boolean;
  readAt?: string;
  showCompletionToast?: boolean;
}

const footerStyle = {
  bottom: 0,
  display: 'flex',
  minHeight: '85px',
  alignItems: 'center',
  justifyContent: 'center',
  borderTop: tokens.border.neutral,
  background: tokens.bg.default,
} satisfies CSSProperties;

const readingStyle = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  gap: tokens.spacing[1],
} satisfies CSSProperties;

const guideStyle = {
  ...typography.subtitle,
  color: tokens.text.brand,
} satisfies CSSProperties;

const progressStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

const completedStyle = {
  ...typography.subtitle,
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[1],
  color: tokens.text.brand,
} satisfies CSSProperties;

const toastStyle = {
  ...typography.body,
  position: 'fixed',
  right: tokens.layout.gutter,
  bottom: `calc(100px + ${tokens.layout.safeBottom})`,
  left: tokens.layout.gutter,
  zIndex: 20,
  display: 'flex',
  maxWidth: '350px',
  height: '48px',
  margin: '0 auto',
  padding: `0 ${tokens.spacing[4]}`,
  alignItems: 'center',
  gap: tokens.spacing[2],
  borderRadius: tokens.radius.md,
  background: tokens.bg.brand,
  color: tokens.text.onBrand,
} satisfies CSSProperties;

export default function MemberNoticeReadState({
  progress,
  isRead,
  readAt,
  showCompletionToast = false,
}: MemberNoticeReadStateProps) {
  const [isToastDismissed, setIsToastDismissed] = useState(false);

  useEffect(() => {
    if (!showCompletionToast) return;

    const timeoutId = window.setTimeout(() => setIsToastDismissed(true), 2000);

    return () => window.clearTimeout(timeoutId);
  }, [showCompletionToast]);

  return (
    <>
      <PostHogCaptureOnViewed name="notice-read">
        {showCompletionToast && !isToastDismissed ? (
          <div css={toastStyle} role="status">
            <img
              src={checkIcon}
              alt=""
              width={16}
              height={16}
              css={{ filter: 'brightness(0) invert(1)' }}
            />
            읽음으로 표시했어요
          </div>
        ) : null}
      </PostHogCaptureOnViewed>

      <footer css={footerStyle}>
        {isRead ? (
          <div css={completedStyle}>
            <img src={circleCheckIcon} alt="읽음 완료" width={22} height={22} />
            {readAt ? `${readAt}에 읽음` : '읽음 완료'}
          </div>
        ) : (
          <div css={readingStyle}>
            <strong css={guideStyle}>끝까지 읽으면 읽음으로 표시돼요</strong>
            <span css={progressStyle}>지금 {progress}% 읽었어요</span>
          </div>
        )}
      </footer>
    </>
  );
}
