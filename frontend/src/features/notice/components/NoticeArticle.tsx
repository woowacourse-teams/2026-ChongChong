import type { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';
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
  return (
    <article css={articleStyle} aria-label="공지 상세">
      <p css={contentStyle}>{notice.content}</p>
    </article>
  );
}
