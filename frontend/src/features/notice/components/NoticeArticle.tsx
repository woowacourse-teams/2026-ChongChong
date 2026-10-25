import { usePostHog } from '@posthog/react';
import type { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';
import ImageGallery from '../../../shared/widgets/ImageGallery';
import type { NoticeDetail } from '../types';

interface Props {
  notice: NoticeDetail;
}

const articleStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSProperties;

const contentStyle = {
  ...typography.subtitle,
  margin: 0,
  color: tokens.text.secondary,
  whiteSpace: 'pre-line',
} satisfies CSSProperties;

export default function NoticeArticle({ notice }: Props) {
  const posthog = usePostHog();
  return (
    <article css={articleStyle} aria-label="공지 상세">
      <p css={contentStyle}>{notice.content}</p>
      <ImageGallery
        attachments={notice.attachments}
        onImageError={(file) =>
          posthog?.capture('attachment_image_load_failed', {
            content_type: 'notice',
            attachment_kind: 'image',
            file_id: file.fileId,
            mime_type: file.contentType,
            file_size_bytes: file.size,
          })
        }
      />
    </article>
  );
}
