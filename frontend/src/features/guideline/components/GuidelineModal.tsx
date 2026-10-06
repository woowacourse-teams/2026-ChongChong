import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router';
import { CSSObject } from '@emotion/react';
import { tokens, typography } from '../../../styles/global';
import { useBodyScrollLock } from '../../../shared/hooks/useBodyScrollLock';
import { guidelinePages } from '../guidelinePages';
import { useGuidelineNavigation } from '../hooks/useGuidelineNavigation';
import { useGuidelineEnvironment } from '../hooks/useGuidelineEnvironment';
import GuidelineNavigation from './GuidelineNavigation';
import GuidelineDismissButton from './GuidelineDismissButton';

interface Props {
  page: number;
  onPageChange: (page: number) => void;
  onClose: () => void;
  onDismiss: () => void;
}

const dialogStyle = {
  boxSizing: 'border-box',
  width: `min(80vw, calc(${tokens.screenSize.default} * 0.8))`,
  height: 'min(700px, 84dvh)',
  minHeight: '50dvh',
  maxHeight: 'calc(100dvh - 24px)',
  padding: 0,
  margin: 'auto',
  overflow: 'hidden',
  border: tokens.border.neutral,
  borderRadius: tokens.radius.xl,
  boxShadow: tokens.shadow[3],
  background: tokens.bg.default,
  color: tokens.text.default,
  fontFamily: tokens.fontFamily.base,
  '&[open]': { display: 'grid', gridTemplateRows: 'auto minmax(0, 1fr) auto' },
  '&::backdrop': { background: tokens.bg.dim },
  '& button:focus-visible, & a:focus-visible': {
    outline: `2px solid ${tokens.text.default}`,
    outlineOffset: '3px',
  },
  '& h2:focus': { outline: 'none' },
} satisfies CSSObject;

export default function GuidelineModal({ page, onPageChange, onClose, onDismiss }: Props) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const previousFocusRef = useRef<Element | null>(null);
  const contentRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const environment = useGuidelineEnvironment();
  const { Component, label } = guidelinePages[page - 1];
  const { goNext } = useGuidelineNavigation(page, guidelinePages.length, onPageChange);

  useBodyScrollLock();

  useEffect(() => {
    // StrictMode의 effect 재실행에서도 모달을 열기 전 초점을 유지한다.
    previousFocusRef.current ??= document.activeElement;
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) dialog.showModal();

    return () => {
      const previousFocus = previousFocusRef.current;
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus();
    };
  }, []);

  useEffect(() => {
    const content = contentRef.current;
    if (!content) return;
    const scrollArea = content.querySelector<HTMLElement>('[data-guideline-scroll]');
    if (scrollArea) scrollArea.scrollTop = 0;
    const heading = content.querySelector('h2');
    heading?.setAttribute('tabindex', '-1');
    heading?.focus({ preventScroll: true });
  }, [page]);

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="guideline-page-title"
      css={dialogStyle}
      onClose={onClose}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <header
        css={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: tokens.spacing[4],
          padding: `${tokens.spacing[2]} ${tokens.spacing[3]}`,
          borderBottom: tokens.border.neutral,
        }}
      >
        <GuidelineDismissButton onClick={onDismiss} />
        <button
          type="button"
          aria-label="가이드 닫기"
          onClick={onClose}
          css={{
            display: 'grid',
            placeItems: 'center',
            width: '44px',
            height: '44px',
            flexShrink: 0,
            border: 0,
            borderRadius: tokens.radius.full,
            background: 'transparent',
            color: tokens.text.muted,
            fontSize: tokens.fontSize[24],
            cursor: 'pointer',
          }}
        >
          <span aria-hidden="true">×</span>
        </button>
      </header>
      <div
        ref={contentRef}
        css={{
          display: 'flex',
          flexDirection: 'column',
          minHeight: 0,
          overflow: 'hidden',
          padding: `${tokens.spacing[5]} ${tokens.spacing[5]} ${tokens.spacing[3]}`,
        }}
      >
        <p
          css={{
            ...typography.label,
            color: tokens.text.brand,
            textAlign: 'center',
            margin: 0,
            flexShrink: 0,
          }}
        >
          {label}
        </p>
        <Component
          {...environment}
          onNext={goNext}
          onClose={onClose}
          onJoinStudy={() => navigate('/studies/join')}
          onNotificationSettings={() => navigate('/studies/mypage')}
        />
      </div>
      <footer
        css={{
          borderTop: tokens.border.neutral,
          padding: `${tokens.spacing[3]} ${tokens.spacing[5]} ${tokens.spacing[2]}`,
        }}
      >
        <GuidelineNavigation
          page={page}
          pageCount={guidelinePages.length}
          onPageChange={onPageChange}
        />
      </footer>
    </dialog>
  );
}
