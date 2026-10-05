import Button from '../../../../shared/ui/Button';
import { tokens, typography } from '../../../../styles/global';
import type { GuidelinePageProps } from '../../types';
import GuidelineArtwork from '../GuidelineArtwork';
import GuidelinePageLayout from '../GuidelinePageLayout';

export default function CompletePage({ pwa, notification, onClose }: GuidelinePageProps) {
  const notificationEnabled = notification.status === 'enabled' && notification.permissionGranted;
  const allComplete = pwa.isStandalone && notificationEnabled;
  const statuses = [
    {
      label: '앱으로 실행',
      checked: pwa.isStandalone,
      status: pwa.isStandalone ? '사용 중' : '브라우저 이용 중',
      description: pwa.isStandalone
        ? '앱으로 잘 들어오셨어요!'
        : pwa.platform === 'ios-safari' || pwa.platform === 'ios-other'
          ? '홈 화면의 앱으로 열어야 알림을 받을 수 있어요.'
          : '앱으로 열면 브라우저 메뉴 없이 이용할 수 있어요.',
    },
    {
      label: '푸시 알림',
      checked: notificationEnabled,
      status: notificationEnabled
        ? '구독 중'
        : notification.status === 'checking'
          ? '확인 중'
          : notification.status === 'error'
            ? '확인 필요'
            : '구독 안 됨',
      description: notificationEnabled
        ? '푸시 알림을 받을 준비가 되었어요.'
        : notification.status === 'checking'
          ? '이 기기의 알림 설정을 확인하고 있어요.'
          : notification.status === 'error'
            ? '알림 설정을 확인하지 못했어요. 잠시 후 다시 확인해 주세요.'
            : '새 공지와 일정의 푸시 알림을 받을 수 없어요.',
    },
  ];

  return (
    <GuidelinePageLayout
      artwork={<GuidelineArtwork theme="complete" />}
      title={allComplete ? '이제 준비 완료!' : '마지막으로 확인해요'}
      description={allComplete ? '축하해요! 준비를 마쳤어요.' : '현재 설정을 확인해 보세요.'}
      action={
        <Button variant="brandSolid" size="large" onClick={onClose}>
          스터디 시작하기
        </Button>
      }
    >
      <dl
        css={{
          ...typography.bodyStrong,
          display: 'grid',
          gap: tokens.spacing[2],
          margin: 0,
          padding: tokens.spacing[3],
          border: tokens.border.default,
          borderRadius: tokens.radius.md,
        }}
      >
        {statuses.map(({ label, checked, status, description }) => (
          <div key={label}>
            <dt css={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              {label}
              <svg
                role="img"
                aria-label={`${label}: ${status}`}
                width="24"
                height="24"
                viewBox="0 0 28 28"
                css={{ color: checked ? '#007b48' : tokens.text.muted }}
              >
                <circle
                  cx="14"
                  cy="14"
                  r="14"
                  fill={checked ? tokens.bg.brandSubtle : tokens.bg.subtle}
                />
                <path
                  d={checked ? 'm8 14 4 4 8-8' : 'M9 14h10'}
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </dt>
            <dd
              css={{
                ...typography.caption,
                color: tokens.text.secondary,
                margin: `${tokens.spacing[1]} 0 0`,
              }}
            >
              {description}
            </dd>
          </div>
        ))}
      </dl>
    </GuidelinePageLayout>
  );
}
