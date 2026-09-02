import type { CSSProperties } from 'react';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
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

export default function NoticeArticle({ notice, hasTopMargin = true }: Props) {
  return (
    <article css={{ ...articleStyle, marginTop: hasTopMargin ? articleStyle.marginTop : 0 }}>
      <h2 css={titleStyle}>{notice.title}</h2>
      <div css={authorRowStyle}>
        <img src={notice.profileImageUrl || profileIcon} alt="" width={28} height={28} />
        <span css={authorStyle}>
          {notice.writer} · {formatRelativeTime(notice.createdAt)}
        </span>
      </div>
      <p css={contentStyle}>{notice.content}</p>
    </article>
  );
}
