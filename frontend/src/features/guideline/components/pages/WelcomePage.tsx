import Button from '../../../../shared/ui/Button';
import { tokens, typography } from '../../../../styles/global';
import type { GuidelinePageProps } from '../../types';
import GuidelineArtwork from '../GuidelineArtwork';
import GuidelinePageLayout from '../GuidelinePageLayout';

export default function WelcomePage({ onNext }: GuidelinePageProps) {
  return (
    <GuidelinePageLayout
      artwork={<GuidelineArtwork theme="welcome" />}
      title="총총에 오신 걸 환영해요!"
      description="함께 공부하는 매일, 총총이 챙겨 드릴게요. 시작하기 전에 이용 방법을 살펴볼까요?"
      action={
        <Button variant="brandSolid" size="large" onClick={onNext}>
          시작하기
        </Button>
      }
    >
      <ul
        css={{
          ...typography.body,
          display: 'grid',
          gap: tokens.spacing[3],
          margin: 0,
          padding: tokens.spacing[4],
          listStyle: 'none',
          background: tokens.bg.subtle,
          borderRadius: tokens.radius.md,
        }}
      >
        {[
          '홈 화면에서 앱처럼 열기',
          '중요한 소식 알림으로 받기',
          '초대 링크로 스터디 참여하기',
        ].map((item, index) => (
          <li key={item} css={{ display: 'flex', gap: tokens.spacing[2] }}>
            <span css={{ color: tokens.text.brand, fontWeight: tokens.fontWeight.semibold }}>
              {index + 1}
            </span>
            {item}
          </li>
        ))}
      </ul>
    </GuidelinePageLayout>
  );
}
