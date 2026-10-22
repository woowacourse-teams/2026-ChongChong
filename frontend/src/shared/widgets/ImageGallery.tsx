import { useState } from 'react';
import type { Attachment } from '../modules/files/types';
import { tokens } from '../../styles/global';

interface Props {
  attachments: Attachment[];
  onReload: () => Promise<unknown>;
}

export default function ImageGallery({ attachments, onReload }: Props) {
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
            <GalleryImage key={file.imageUrl} file={file} onReload={onReload} single={single} />
          </li>
        ))}
      </ul>
    </section>
  );
}

function GalleryImage({
  file,
  onReload,
  single,
}: {
  file: Attachment & { imageUrl: string };
  onReload: () => Promise<unknown>;
  single: boolean;
}) {
  const [failed, setFailed] = useState(false);
  const [retrying, setRetrying] = useState(false);
  return (
    <figure css={{ margin: 0 }}>
      {failed ? (
        <div css={{ padding: tokens.spacing[4], textAlign: 'center' }}>
          <p>이미지를 불러오지 못했어요.</p>
          <button
            type="button"
            disabled={retrying}
            onClick={async () => {
              setRetrying(true);
              try {
                await onReload();
                setFailed(false);
              } catch {
                /* 실패 안내를 유지하고 사용자가 다시 시도할 수 있게 한다. */
              } finally {
                setRetrying(false);
              }
            }}
          >
            이미지 다시 불러오기
          </button>
        </div>
      ) : (
        <img
          src={file.imageUrl}
          alt={file.originalFileName}
          onError={() => setFailed(true)}
          css={{
            display: 'block',
            width: '100%',
            aspectRatio: single ? '350 / 375' : '1',
            objectFit: 'cover',
            borderRadius: 12,
          }}
        />
      )}
    </figure>
  );
}
