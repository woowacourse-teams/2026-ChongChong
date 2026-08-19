import type { CSSProperties } from 'react';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import { tokens, typography } from '../../../styles/global';

interface NoticeArticleProps {
  title: string;
  author: string;
  createdAt: string;
  content: string;
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

const authorRowStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: tokens.spacing[2],
  marginTop: tokens.spacing[1],
} satisfies CSSProperties;

const authorStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

const contentStyle = {
  ...typography.subtitle,
  margin: `${tokens.spacing[4]} 0 0`,
  color: tokens.text.secondary,
  whiteSpace: 'pre-line',
} satisfies CSSProperties;

export default function NoticeArticle({
  title,
  author,
  createdAt,
  content,
  hasTopMargin = true,
}: NoticeArticleProps) {
  return (
    <article css={{ ...articleStyle, marginTop: hasTopMargin ? articleStyle.marginTop : 0 }}>
      <h2 css={titleStyle}>{title}</h2>
      <div css={authorRowStyle}>
        <img src={profileIcon} alt={`${author} 프로필`} width={28} height={28} />
        <span css={authorStyle}>
          {author} · {createdAt}
        </span>
      </div>
      <p css={contentStyle}>{content}</p>
    </article>
  );
}
