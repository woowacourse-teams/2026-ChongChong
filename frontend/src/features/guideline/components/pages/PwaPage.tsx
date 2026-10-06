import { useEffect, useRef } from 'react';
import Button from '../../../../shared/ui/Button';
import { tokens, typography } from '../../../../styles/global';
import type { GuidelinePageProps } from '../../types';
import GuidelineArtwork from '../GuidelineArtwork';
import GuidelinePageLayout from '../GuidelinePageLayout';
import PwaInstallGuide from '../PwaInstallGuide';
import { getPwaInstallGuide } from '../../utils/pwaInstallGuide';

export default function PwaPage({ pwa, onNext }: GuidelinePageProps) {
  const isReady = pwa.isStandalone || pwa.isInstalled;
  const isIos = pwa.platform === 'ios-safari' || pwa.platform === 'ios-other';
  const guideRef = useRef<HTMLElement>(null);
  const guide = getPwaInstallGuide(pwa.platform);

  function focusGuide() {
    guideRef.current?.focus({ preventScroll: true });
    guideRef.current?.scrollIntoView?.({ block: 'start', inline: 'nearest' });
  }

  useEffect(() => {
    if (pwa.guideOpen && !isReady) focusGuide();
  }, [pwa.guideOpen, isReady]);

  return (
    <GuidelinePageLayout
      artwork={<GuidelineArtwork theme="pwa" />}
      title="홈 화면에서 바로 만나요"
      description={guide.description}
      action={
        <Button
          variant="brandSolid"
          size="large"
          onClick={() => {
            if (isIos) {
              onNext();
              return;
            }
            if (pwa.guideOpen) focusGuide();
            void pwa.install();
          }}
          disabled={pwa.isInstalling || isReady}
          aria-busy={pwa.isInstalling}
        >
          {isReady
            ? '홈 화면 앱 준비 완료'
            : pwa.isInstalling
              ? '추가하는 중...'
              : guide.actionLabel}
        </Button>
      }
    >
      {!isReady && <PwaInstallGuide ref={guideRef} platform={pwa.platform} />}
      {pwa.message && (
        <p
          role="status"
          css={{
            ...typography.caption,
            margin: 0,
            color: tokens.text.secondary,
            textAlign: 'center',
          }}
        >
          {pwa.message}
        </p>
      )}
    </GuidelinePageLayout>
  );
}
