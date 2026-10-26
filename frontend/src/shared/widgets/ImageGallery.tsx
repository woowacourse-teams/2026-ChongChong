import { useEffect, useRef, useState, type CSSProperties } from 'react';
import errorImage from '../assets/icons/image-load-error.webp';
import type { Attachment } from '../modules/files/types';
import { tokens } from '../../styles/global';
import { useBodyScrollLock } from '../hooks/useBodyScrollLock';
import type { CSSObject } from '@emotion/react';

interface Props {
  attachments: Attachment[];
  onImageError?: (file: Attachment) => void;
}

const gallerySectionStyle = {
  minWidth: 0,
  maxWidth: '100%',
  overflowX: 'auto',
  scrollbarWidth: 'none',
  '&::-webkit-scrollbar': { display: 'none' },
} satisfies CSSObject;

const galleryListStyle = {
  display: 'flex',
  gap: tokens.spacing[2],
  listStyle: 'none',
  margin: 0,
  padding: 0,
} satisfies CSSProperties;

const galleryItemStyle = (single: boolean) =>
  ({
    flex: single ? '0 0 100%' : '0 0 200px',
    minWidth: 0,
  }) satisfies CSSProperties;

const galleryFigureStyle = { margin: 0 } satisfies CSSProperties;

const galleryErrorStyle = (single: boolean) =>
  ({
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    aspectRatio: single ? '350 / 375' : '1',
    borderRadius: 12,
  }) satisfies CSSProperties;

const galleryImageButtonStyle = {
  display: 'block',
  width: '100%',
  padding: 0,
  border: 0,
  background: 'none',
} satisfies CSSProperties;

const galleryImageStyle = (single: boolean) =>
  ({
    display: 'block',
    width: '100%',
    aspectRatio: single ? '350 / 375' : '1',
    objectFit: 'contain',
    backgroundColor: tokens.bg.subtle,
    borderRadius: 12,
  }) satisfies CSSProperties;

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

export default function ImageGallery({ attachments, onImageError }: Props) {
  const [expandedImage, setExpandedImage] = useState<(Attachment & { imageUrl: string }) | null>(
    null,
  );
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const images = [...attachments]
    .filter((file): file is Attachment & { imageUrl: string } => file.imageUrl !== null)
    .sort((a, b) => a.sortOrder - b.sortOrder);
  if (!images.length) return null;
  const single = images.length === 1;
  const expandedIndex = images.findIndex((file) => file.fileId === expandedImage?.fileId);

  return (
    <>
      <section aria-label="첨부 이미지" tabIndex={single ? undefined : 0} css={gallerySectionStyle}>
        <ul role="list" css={galleryListStyle}>
          {images.map((file) => (
            <li key={file.fileId} css={galleryItemStyle(single)}>
              <GalleryImage
                key={file.imageUrl}
                file={file}
                single={single}
                onImageError={onImageError}
                onOpen={(button) => {
                  triggerRef.current = button;
                  setExpandedImage(file);
                }}
              />
            </li>
          ))}
        </ul>
      </section>
      {expandedImage && (
        <ExpandedImage
          file={images[expandedIndex] ?? expandedImage}
          previousFile={images[expandedIndex - 1]}
          nextFile={images[expandedIndex + 1]}
          onNavigate={setExpandedImage}
          onClose={() => {
            setExpandedImage(null);
            triggerRef.current?.focus();
          }}
        />
      )}
    </>
  );
}

function GalleryImage({
  file,
  single,
  onImageError,
  onOpen,
}: {
  file: Attachment & { imageUrl: string };
  single: boolean;
  onImageError?: Props['onImageError'];
  onOpen: (button: HTMLButtonElement) => void;
}) {
  const failureReported = useRef(false);
  const [failed, setFailed] = useState(false);
  return (
    <figure css={galleryFigureStyle}>
      {failed ? (
        <div css={galleryErrorStyle(single)}>
          <img src={errorImage} alt="이미지를 불러오지 못했어요." width={80} height={80} />
          <p aria-hidden="true">이미지를 불러오지 못했어요.</p>
        </div>
      ) : (
        <button
          type="button"
          aria-label={`${file.originalFileName} 크게 보기`}
          aria-haspopup="dialog"
          onClick={(event) => onOpen(event.currentTarget)}
          css={galleryImageButtonStyle}
        >
          <img
            src={file.imageUrl}
            alt={file.originalFileName}
            onError={() => {
              setFailed(true);
              if (failureReported.current) return;
              failureReported.current = true;
              onImageError?.(file);
            }}
            css={galleryImageStyle(single)}
          />
        </button>
      )}
    </figure>
  );
}

function ExpandedImage({
  file,
  previousFile,
  nextFile,
  onNavigate,
  onClose,
}: {
  file: Attachment & { imageUrl: string };
  previousFile?: Attachment & { imageUrl: string };
  nextFile?: Attachment & { imageUrl: string };
  onNavigate: (file: Attachment & { imageUrl: string }) => void;
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
      aria-label={`${file.originalFileName} 크게 보기`}
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
      <img src={file.imageUrl} alt={file.originalFileName} css={expandedImageStyle} />
      {previousFile && (
        <NavigationArrow direction="previous" onClick={() => onNavigate(previousFile)} />
      )}
      {nextFile && <NavigationArrow direction="next" onClick={() => onNavigate(nextFile)} />}
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
