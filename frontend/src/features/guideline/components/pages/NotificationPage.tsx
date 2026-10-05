import Button from '../../../../shared/ui/Button';
import { tokens, typography } from '../../../../styles/global';
import type { GuidelinePageProps } from '../../types';
import GuidelineArtwork from '../GuidelineArtwork';
import GuidelinePageLayout from '../GuidelinePageLayout';

export default function NotificationPage({
  notification,
  onNotificationSettings,
}: GuidelinePageProps) {
  const { status } = notification;
  const isEnabled = status === 'enabled';
  const isChecking = status === 'checking';
  const hasError = status === 'error' || status === 'denied';

  return (
    <GuidelinePageLayout
      artwork={<GuidelineArtwork theme="notification" />}
      title="중요한 소식, 놓치지 않게"
      description={
        isEnabled
          ? '이미 푸시 알림을 구독하고 있어요. 새 공지와 다가오는 일정을 알림으로 받아 보세요.'
          : '새 공지와 다가오는 일정을 푸시 알림으로 받아 보세요. 마이페이지에서 알림을 켤 수 있어요.'
      }
      action={
        <Button
          variant={isEnabled ? 'brandOutline' : 'brandSolid'}
          size="large"
          onClick={onNotificationSettings}
          disabled={isEnabled || isChecking}
          aria-busy={isChecking}
          css={{ gap: tokens.spacing[2] }}
        >
          {isEnabled && <span aria-hidden="true">✓</span>}
          {isEnabled ? '알림 구독 중' : isChecking ? '알림 상태 확인 중…' : '알림 켜기'}
        </Button>
      }
    >
      {!isEnabled && (
        <div
          css={{
            ...typography.body,
            padding: tokens.spacing[4],
            borderRadius: tokens.radius.md,
            background: tokens.bg.subtle,
          }}
        >
          <p css={{ ...typography.bodyStrong, margin: 0 }}>마이페이지 → 푸시 알림</p>
          <p css={{ margin: `${tokens.spacing[2]} 0 0`, color: tokens.text.secondary }}>
            스위치를 켜고, 브라우저에서 알림 권한을 물으면 ‘허용’을 눌러 주세요.
          </p>
        </div>
      )}
      {notification.message && (
        <p
          role={hasError ? 'alert' : 'status'}
          css={{
            ...typography.caption,
            margin: 0,
            color: hasError ? tokens.text.critical : tokens.text.secondary,
            textAlign: 'center',
          }}
        >
          {notification.message}
        </p>
      )}
    </GuidelinePageLayout>
  );
}
