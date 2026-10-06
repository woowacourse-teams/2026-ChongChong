import { useState, type Ref } from 'react';
import Button from '../../../shared/ui/Button';
import { tokens, typography } from '../../../styles/global';
import {
  getPwaInstallGuide,
  getSafariInstallUrl,
  type PwaGuideIcon,
  type PwaPlatform,
} from '../utils/pwaInstallGuide';

function StepIcon({ icon }: { icon: PwaGuideIcon }) {
  const shapes = {
    menu: (
      <>
        <circle cx="5" cy="12" r="1" />
        <circle cx="12" cy="12" r="1" />
        <circle cx="19" cy="12" r="1" />
      </>
    ),
    share: (
      <>
        <path d="M12 15V3m-4 4 4-4 4 4" />
        <path d="M7 10H4v11h16V10h-3" />
      </>
    ),
    add: (
      <>
        <rect x="3" y="3" width="18" height="18" rx="4" />
        <path d="M12 7v10M7 12h10" />
      </>
    ),
    app: (
      <>
        <rect x="5" y="2" width="14" height="20" rx="3" />
        <path d="M10 18h4m-6-8 3 3 5-5" />
      </>
    ),
    browser: (
      <>
        <circle cx="12" cy="12" r="10" />
        <path d="m16 8-2 6-6 2 2-6z" />
      </>
    ),
  };
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {shapes[icon]}
    </svg>
  );
}

function SafariHandoff() {
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copying' | 'copied' | 'failed'>('idle');
  const url = getSafariInstallUrl();

  async function copyUrl() {
    setCopyStatus('copying');
    try {
      if (!navigator.clipboard?.writeText) throw new Error('Clipboard unavailable');
      await navigator.clipboard.writeText(url);
      setCopyStatus('copied');
    } catch {
      setCopyStatus('failed');
    }
  }

  return (
    <div
      css={{
        display: 'grid',
        gap: tokens.spacing[2],
        padding: tokens.spacing[3],
        borderRadius: tokens.radius.md,
        background: tokens.bg.brandSubtle,
      }}
    >
      <p css={{ ...typography.body, margin: 0 }}>
        주소를 복사한 뒤 Safari를 열고 주소창에 붙여 넣어 주세요.
      </p>
      <input
        aria-label="Safari에서 열 주소"
        value={url}
        readOnly
        onFocus={(event) => event.currentTarget.select()}
        onClick={(event) => event.currentTarget.select()}
        css={{
          ...typography.caption,
          boxSizing: 'border-box',
          width: '100%',
          minWidth: 0,
          minHeight: '44px',
          padding: tokens.spacing[2],
          border: tokens.border.neutral,
          borderRadius: tokens.radius.sm,
          background: tokens.bg.default,
          color: tokens.text.default,
        }}
      />
      <Button
        variant="neutralOutline"
        size="large"
        onClick={() => void copyUrl()}
        disabled={copyStatus === 'copying'}
      >
        {copyStatus === 'copied'
          ? '주소 다시 복사'
          : copyStatus === 'copying'
            ? '복사하는 중...'
            : '주소 복사'}
      </Button>
      {(copyStatus === 'copied' || copyStatus === 'failed') && (
        <p role="status" css={{ ...typography.caption, margin: 0, color: tokens.text.secondary }}>
          {copyStatus === 'copied'
            ? '주소를 복사했어요. Safari 주소창에 붙여 넣어 주세요.'
            : '자동으로 복사하지 못했어요. 위 주소를 선택해 직접 복사한 뒤 Safari에 붙여 넣어 주세요.'}
        </p>
      )}
    </div>
  );
}

export default function PwaInstallGuide({
  platform,
  ref,
}: {
  platform: PwaPlatform;
  ref?: Ref<HTMLElement>;
}) {
  const guide = getPwaInstallGuide(platform);

  return (
    <section
      ref={ref}
      id="pwa-install-guide"
      aria-labelledby="pwa-install-guide-title"
      tabIndex={-1}
      css={{
        display: 'grid',
        gap: tokens.spacing[3],
        outline: 'none',
        scrollMarginTop: tokens.spacing[2],
      }}
    >
      <h3 id="pwa-install-guide-title" css={{ ...typography.bodyStrong, margin: 0 }}>
        {guide.title}
      </h3>
      {platform === 'ios-other' && <SafariHandoff />}
      <ol
        css={{ listStyle: 'none', display: 'grid', gap: tokens.spacing[3], margin: 0, padding: 0 }}
      >
        {guide.steps.map((step, index) => (
          <li
            key={step.title}
            css={{ display: 'flex', gap: tokens.spacing[3], alignItems: 'flex-start' }}
          >
            <span
              css={{
                flexShrink: 0,
                display: 'grid',
                placeItems: 'center',
                width: '40px',
                height: '40px',
                borderRadius: tokens.radius.md,
                background: tokens.bg.subtle,
                color: tokens.text.default,
              }}
            >
              <StepIcon icon={step.icon} />
            </span>
            <div css={{ minWidth: 0 }}>
              <p css={{ ...typography.bodyStrong, margin: 0 }}>
                <span aria-hidden="true">{index + 1}. </span>
                <span>{step.title}</span>
              </p>
              <p
                css={{
                  ...typography.caption,
                  margin: `${tokens.spacing[1]} 0 0`,
                  color: tokens.text.secondary,
                }}
              >
                {step.description}
              </p>
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}
