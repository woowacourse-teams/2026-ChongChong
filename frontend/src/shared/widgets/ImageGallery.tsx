import { useRef, useState } from 'react';
import errorImage from '../assets/icons/image-load-error.webp';
import type { Attachment } from '../modules/files/types';
import { tokens } from '../../styles/global';

interface Props {
  attachments: Attachment[];
  onImageError?: (file: Attachment) => void;
}

export default function ImageGallery({ attachments, onImageError }: Props) {
  const images = [...attachments]
    .filter((file): file is Attachment & { imageUrl: string } => file.imageUrl !== null)
    .sort((a, b) => a.sortOrder - b.sortOrder);
  if (!images.length) return null;
  const single = images.length === 1;

  return (
    <section
      aria-label="첨부 이미지"
      tabIndex={single ? undefined : 0}
      css={{
        minWidth: 0,
        maxWidth: '100%',
        overflowX: 'auto',
        scrollbarWidth: 'none',
        '&::-webkit-scrollbar': { display: 'none' },
      }}
    >
      <ul
        role="list"
        css={{ display: 'flex', gap: tokens.spacing[2], listStyle: 'none', margin: 0, padding: 0 }}
      >
        {images.map((file) => (
          <li key={file.fileId} css={{ flex: single ? '0 0 100%' : '0 0 200px', minWidth: 0 }}>
            <GalleryImage
              key={file.imageUrl}
              file={file}
              single={single}
              onImageError={onImageError}
            />
          </li>
        ))}
      </ul>
    </section>
  );
}

function GalleryImage({
  file,
  single,
  onImageError,
}: {
  file: Attachment & { imageUrl: string };
  single: boolean;
  onImageError?: Props['onImageError'];
}) {
  const failureReported = useRef(false);
  const [failed, setFailed] = useState(false);
  return (
    <figure css={{ margin: 0 }}>
      {failed ? (
        <div
          css={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            width: '100%',
            aspectRatio: single ? '350 / 375' : '1',
            borderRadius: 12,
          }}
        >
          <img src={errorImage} alt="이미지를 불러오지 못했어요." width={80} height={80} />
          <p aria-hidden="true">이미지를 불러오지 못했어요.</p>
        </div>
      ) : (
        <img
          src={file.imageUrl}
          alt={file.originalFileName}
          onError={() => {
            setFailed(true);
            if (failureReported.current) return;
            failureReported.current = true;
            onImageError?.(file);
          }}
          css={{
            display: 'block',
            width: '100%',
            aspectRatio: single ? '350 / 375' : '1',
            objectFit: 'contain',
            backgroundColor: tokens.bg.subtle,
            borderRadius: 12,
          }}
        />
      )}
    </figure>
  );
}
