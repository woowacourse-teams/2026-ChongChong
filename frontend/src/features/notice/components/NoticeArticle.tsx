import type { CSSProperties } from 'react';
import { formatRelativeTime } from '../../../shared/utils/formatDate';
import { tokens, typography } from '../../../styles/global';
import type { NoticeDetail } from '../types';

interface Props {
  notice: NoticeDetail;
  hasTopMargin?: boolean;
}

const articleStyle = {
  display: 'flex',
  flexDirection: 'column',
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

const titleStyle = {
  ...typography.headline,
  margin: 0,
  color: tokens.text.primary,
} satisfies CSSProperties;

const createdAtStyle = {
  ...typography.footnote,
  marginTop: tokens.spacing[1],
  color: tokens.text.muted,
} satisfies CSSProperties;

const contentStyle = {
  ...typography.subtitle,
  margin: `${tokens.spacing[4]} 0 0`,
  color: tokens.text.secondary,
  whiteSpace: 'pre-line',
} satisfies CSSProperties;

export default function NoticeArticle({ notice, hasTopMargin = true }: Props) {
  return (
    <article css={{ ...articleStyle, marginTop: hasTopMargin ? articleStyle.marginTop : 0 }}>
      <h2 css={titleStyle}>{notice.title}</h2>
      <span css={createdAtStyle}>{formatRelativeTime(notice.createdAt)}</span>
      <p css={contentStyle}>{notice.content}</p>
    </article>
  );
}
