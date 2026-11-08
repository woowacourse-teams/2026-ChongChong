import type { CSSProperties } from 'react';
import type { Comment } from '../../modules/comments/responseSchemas';
import Avatar from '../../ui/Avatar';
import { tokens, typography } from '../../../styles/global';
import defaultProfile from '../../assets/unknown-profile.svg';

const rowStyle = {
  display: 'flex',
  gap: tokens.spacing[3],
  paddingBottom: tokens.spacing[3],
  borderBottom: tokens.border.default,
} satisfies CSSProperties;

const infoStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
  minWidth: 0,
  flex: 1,
  ...typography.body,
  color: tokens.text.primary,
  overflowWrap: 'anywhere',
} satisfies CSSProperties;

const timeStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

function commentDate(createdAt: string) {
  const date = new Date(createdAt);
  return `${date.getMonth() + 1}월 ${date.getDate()}일 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

export default function CommentItem({ comment }: { comment: Comment }) {
  const suffix = comment.isMine ? ' · 나' : comment.author.role === 'LEADER' ? ' · 리더' : '';

  return (
    <li css={rowStyle}>
      <Avatar
        size={32}
        src={comment.author.isDeparted ? null : comment.author.profileImageUrl}
        alt=""
        fallback={<img src={defaultProfile} alt="" width={32} height={32} />}
      />
      <div css={infoStyle}>
        <p css={{ margin: 0 }}>
          {comment.author.name}
          {suffix}
        </p>
        <p css={{ margin: 0, whiteSpace: 'pre-wrap' }}>{comment.content}</p>
        <time dateTime={comment.createdAt} css={timeStyle}>
          {commentDate(comment.createdAt)}
          {comment.isEdited ? ' · 수정됨' : ''}
        </time>
      </div>
    </li>
  );
}
