import type { CSSProperties, Ref } from 'react';
import type { Comment } from '../../modules/comments/responseSchemas';
import Avatar from '../../ui/Avatar';
import { tokens, typography } from '../../../styles/global';
import defaultProfile from '../../assets/unknown-profile.svg';
import CommentMenu from './CommentMenu';

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

interface Props {
  comment: Comment;
  onEdit?: (comment: Comment, button: HTMLButtonElement | null) => void;
  onDelete?: (comment: Comment, button: HTMLButtonElement | null) => void;
  ref?: Ref<HTMLLIElement>;
}

export default function CommentItem({ comment, onEdit, onDelete, ref }: Props) {
  const suffix = comment.isMine ? ' · 나' : comment.author.role === 'LEADER' ? ' · 리더' : '';

  return (
    <li css={rowStyle} ref={ref}>
      <Avatar
        size={32}
        src={comment.author.isDeparted ? null : comment.author.profileImageUrl}
        alt=""
        fallback={<img src={defaultProfile} alt="" width={32} height={32} />}
      />
      <div css={infoStyle}>
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: tokens.spacing[2],
          }}
        >
          <p css={{ margin: 0 }}>
            {comment.author.name}
            {suffix}
          </p>
          {onEdit && onDelete && (comment.canEdit || comment.canDelete) && (
            <CommentMenu
              comment={comment}
              onEdit={(button) => onEdit(comment, button)}
              onDelete={(button) => onDelete(comment, button)}
            />
          )}
        </div>
        <p css={{ margin: 0, whiteSpace: 'pre-wrap' }}>{comment.content}</p>
        <time dateTime={comment.createdAt} css={timeStyle}>
          {commentDate(comment.createdAt)}
          {comment.isEdited ? ' · 수정됨' : ''}
        </time>
      </div>
    </li>
  );
}
