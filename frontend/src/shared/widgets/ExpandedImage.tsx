import { useEffect, useRef, type CSSProperties } from 'react';
import type { CSSObject } from '@emotion/react';
import { useBodyScrollLock } from '../hooks/useBodyScrollLock';

export interface ExpandableImage {
  id: string;
  src: string;
  name: string;
}

const expandedDialogStyle = {
  boxSizing: 'border-box',
  position: 'fixed',
  inset: 0,
  width: '100vw',
  height: '100dvh',
  maxWidth: 'none',
  maxHeight: 'none',
  margin: 0,
  padding: 0,
  border: 0,
  background: 'transparent',
  overflow: 'hidden',
  '&::backdrop': { background: 'rgba(0, 0, 0, 0.8)' },
} satisfies CSSObject;

const expandedCloseButtonStyle = {
  position: 'absolute',
  top: 24,
  right: 24,
  zIndex: 1,
  width: 48,
  height: 48,
  padding: 0,
  color: '#fff',
  fontSize: 24,
  cursor: 'pointer',
} satisfies CSSProperties;

const expandedImageStyle = {
  position: 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  display: 'block',
  width: 'auto',
  height: 'auto',
  maxWidth: 'min(80vw, 1240px)',
  maxHeight: 'calc(100dvh - 80px)',
  objectFit: 'contain',
  '@media (max-width: 600px)': { maxWidth: 'calc(100vw - 24px)' },
} satisfies CSSObject;

const navigationButtonStyle = {
  position: 'absolute',
  top: '50%',
  transform: 'translateY(-50%)',
  width: 48,
  height: 48,
  padding: 0,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  border: '1px solid rgba(255, 255, 255, 0.4)',
  borderRadius: '50%',
  background: 'rgba(0, 0, 0, 0.35)',
  color: '#fff',
  fontSize: 24,
  cursor: 'pointer',
} satisfies CSSProperties;

const previousButtonStyle = {
  ...navigationButtonStyle,
  left: 24,
  '@media (max-width: 600px)': { left: 8 },
} satisfies CSSObject;

const nextButtonStyle = {
  ...navigationButtonStyle,
  right: 24,
  '@media (max-width: 600px)': { right: 8 },
} satisfies CSSObject;

export default function ExpandedImage({
  image,
  previousImage,
  nextImage,
  onNavigate,
  onClose,
}: {
  image: ExpandableImage;
  previousImage?: ExpandableImage;
  nextImage?: ExpandableImage;
  onNavigate: (image: ExpandableImage) => void;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    dialogRef.current?.showModal();
  }, []);

  useBodyScrollLock();

  return (
    <dialog
      ref={dialogRef}
      aria-label={`${image.name} 크게 보기`}
      onClose={onClose}
      onClick={(event) => {
        if (event.target === event.currentTarget) dialogRef.current?.close();
      }}
      css={expandedDialogStyle}
    >
      <button
        type="button"
        aria-label="확대 이미지 닫기"
        onClick={() => dialogRef.current?.close()}
        css={expandedCloseButtonStyle}
      >
        ✕
      </button>
      <img src={image.src} alt={image.name} css={expandedImageStyle} />
      {previousImage && (
        <NavigationArrow direction="previous" onClick={() => onNavigate(previousImage)} />
      )}
      {nextImage && <NavigationArrow direction="next" onClick={() => onNavigate(nextImage)} />}
    </dialog>
  );
}

function NavigationArrow({
  direction,
  onClick,
}: {
  direction: 'previous' | 'next';
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      aria-label={direction === 'previous' ? '이전 이미지' : '다음 이미지'}
      onClick={onClick}
      css={direction === 'previous' ? previousButtonStyle : nextButtonStyle}
    >
      <svg
        width="24"
        height="24"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
        focusable="false"
      >
        <path d={direction === 'previous' ? 'M19 12H5m6-6-6 6 6 6' : 'M5 12h14m-6-6 6 6-6 6'} />
      </svg>
    </button>
  );
}
