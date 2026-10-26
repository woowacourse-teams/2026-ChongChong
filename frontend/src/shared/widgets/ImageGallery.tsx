import { useRef, useState, type CSSProperties } from 'react';
import errorImage from '../assets/icons/image-load-error.webp';
import type { Attachment } from '../modules/files/types';
import { tokens } from '../../styles/global';
import type { CSSObject } from '@emotion/react';
import ExpandedImage, { type ExpandableImage } from './ExpandedImage';

interface Props {
  attachments: Attachment[];
  navigationAttachments?: Attachment[];
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

export default function ImageGallery({
  attachments,
  navigationAttachments = attachments,
  onImageError,
}: Props) {
  const [expandedImage, setExpandedImage] = useState<ExpandableImage | null>(null);
  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const images = [...attachments]
    .filter((file): file is Attachment & { imageUrl: string } => file.imageUrl !== null)
    .sort((a, b) => a.sortOrder - b.sortOrder);
  if (!images.length) return null;
  const expandableImages = [...navigationAttachments]
    .filter((file): file is Attachment & { imageUrl: string } => file.imageUrl !== null)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .map((file) => ({
      id: String(file.fileId),
      src: file.imageUrl,
      name: file.originalFileName,
    }));
  const single = images.length === 1;
  const expandedIndex = expandableImages.findIndex((image) => image.id === expandedImage?.id);

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
                  setExpandedImage({
                    id: String(file.fileId),
                    src: file.imageUrl,
                    name: file.originalFileName,
                  });
                }}
              />
            </li>
          ))}
        </ul>
      </section>
      {expandedImage && expandedIndex !== -1 && (
        <ExpandedImage
          image={expandableImages[expandedIndex]}
          previousImage={expandableImages[expandedIndex - 1]}
          nextImage={expandableImages[expandedIndex + 1]}
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
