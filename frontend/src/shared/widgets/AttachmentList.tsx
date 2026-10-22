import type { Attachment } from '../modules/files/types';
import { tokens, typography } from '../../styles/global';
import type { CSSObject } from '@emotion/react';

const rowStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[3],
  minWidth: 0,
  padding: `${tokens.spacing[3]} 0`,
  color: tokens.text.primary,
  textDecoration: 'none',
} satisfies CSSObject;

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Number((bytes / 1024).toFixed(1))} KB`;
  return `${Number((bytes / (1024 * 1024)).toFixed(1))} MB`;
}

function FileDetails({ file }: { file: Attachment }) {
  const extension = file.originalFileName.includes('.')
    ? file.originalFileName.split('.').at(-1)?.toUpperCase()
    : undefined;
  const type = extension && extension.length <= 8 ? extension : '파일';

  return (
    <>
      <span
        css={{
          flex: 1,
          minWidth: 0,
          display: 'flex',
          flexDirection: 'column',
          gap: tokens.spacing[1],
        }}
      >
        <span css={{ ...typography.body, overflowWrap: 'anywhere' }}>{file.originalFileName}</span>
        <span css={{ ...typography.caption, color: tokens.text.muted }}>
          {type} · {formatFileSize(file.size)}
        </span>
      </span>
      {file.imageUrl ? (
        <span css={{ ...typography.caption, color: tokens.text.brand, flexShrink: 0 }}>열기</span>
      ) : (
        <span css={{ ...typography.caption, color: tokens.text.placeholder, flexShrink: 0 }}>
          미리보기 없음
        </span>
      )}
    </>
  );
}

export default function AttachmentList({ attachments }: { attachments: Attachment[] }) {
  if (!attachments.length) return null;

  return (
    <ul aria-label="첨부 파일" css={{ margin: 0, padding: 0, listStyle: 'none' }}>
      {[...attachments]
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map((file) => (
          <li key={file.fileId} css={{ '& + &': { borderTop: tokens.border.default } }}>
            {file.imageUrl ? (
              <a
                href={file.imageUrl}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`${file.originalFileName} 새 탭에서 열기`}
                css={{
                  ...rowStyle,
                  '&:hover': { opacity: 0.7 },
                  '&:focus-visible': {
                    outline: `2px solid ${tokens.text.brand}`,
                    outlineOffset: 2,
                    borderRadius: tokens.radius.sm,
                  },
                }}
              >
                <FileDetails file={file} />
              </a>
            ) : (
              <div css={rowStyle}>
                <FileDetails file={file} />
              </div>
            )}
          </li>
        ))}
    </ul>
  );
}
