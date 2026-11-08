import { useId, useState, type CSSProperties } from 'react';
import { createPortal } from 'react-dom';
import { useInfiniteQuery } from '@tanstack/react-query';
import commentQueries from '../../modules/comments/queries';
import type { CommentTarget } from '../../modules/comments/types';
import BottomSheet from '../../ui/BottomSheet';
import { tokens, typography } from '../../../styles/global';
import CommentItem from './CommentItem';
import closeIcon from '../../assets/close.svg';

interface Props {
  target: CommentTarget;
  title: string;
}

const entryStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  width: '100%',
  minHeight: '48px',
  marginTop: tokens.spacing[5],
  borderTop: tokens.border.default,
  ...typography.body,
  color: tokens.text.primary,
  textAlign: 'left',
  flexShrink: 0,
} satisfies CSSProperties;

const headerStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: tokens.spacing[3],
  flexShrink: 0,
} satisfies CSSProperties;

const titleStyle = {
  ...typography.body,
  margin: 0,
  color: tokens.text.primary,
} satisfies CSSProperties;

const closeStyle = {
  display: 'grid',
  placeItems: 'center',
  minWidth: '32px',
  minHeight: '32px',
  margin: '-7px',
  flexShrink: 0,
} satisfies CSSProperties;

const scrollStyle = {
  minHeight: 0,
  overflowY: 'auto',
  overscrollBehavior: 'contain',
  ...typography.body,
} satisfies CSSProperties;

const listStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[3],
  padding: 0,
  margin: 0,
  listStyle: 'none',
} satisfies CSSProperties;

const statusStyle = {
  paddingBlock: tokens.spacing[4],
  color: tokens.text.secondary,
  ...typography.body,
} satisfies CSSProperties;

const moreStyle = {
  width: '100%',
  minHeight: '48px',
  color: tokens.text.secondary,
  ...typography.body,
} satisfies CSSProperties;

export default function CommentsPanel({ target, title }: Props) {
  const [open, setOpen] = useState(false);
  const titleId = useId();
  const query = useInfiniteQuery(commentQueries.list(target));
  const totalCount = query.data?.pages.at(-1)?.totalCount;
  const comments = query.data?.pages.flatMap((page) => page.comments) ?? [];
  const label = `${title}${totalCount === undefined ? '' : ` ${totalCount}`}`;
  const hasAccessError =
    query.error &&
    'status' in query.error &&
    (query.error.status === 401 || query.error.status === 403 || query.error.status === 404);
  const showList = query.data !== undefined && !hasAccessError;

  return (
    <>
      <button
        type="button"
        css={entryStyle}
        aria-label={`${label} 열기`}
        aria-haspopup="dialog"
        aria-expanded={open}
        onClick={() => setOpen(true)}
      >
        <span>댓글{totalCount === undefined || hasAccessError ? '' : ` ${totalCount}`}</span>
        <span css={{ ...typography.footnote, color: tokens.text.secondary }}>댓글 남기기 ›</span>
      </button>
      {createPortal(
        <BottomSheet
          open={open}
          onClose={() => setOpen(false)}
          aria-labelledby={titleId}
          variant="compact"
          maxHeight="50dvh"
        >
          <header css={headerStyle}>
            <h2 id={titleId} css={titleStyle}>
              {hasAccessError ? title : label}
            </h2>
            <button
              type="button"
              aria-label="댓글 닫기"
              onClick={() => setOpen(false)}
              css={closeStyle}
            >
              <img src={closeIcon} alt="" />
            </button>
          </header>
          <div css={scrollStyle} aria-busy={query.isFetching}>
            {query.isPending && (
              <p role="status" css={statusStyle}>
                댓글을 불러오는 중이에요.
              </p>
            )}
            {query.isError && !query.isFetchNextPageError && (
              <div role="alert" css={statusStyle}>
                <p>{query.error.message}</p>
                <button
                  type="button"
                  css={moreStyle}
                  disabled={query.isFetching}
                  onClick={() => void query.refetch()}
                >
                  다시 시도
                </button>
              </div>
            )}
            {showList && (
              <>
                {comments.length === 0 ? (
                  <p css={statusStyle}>아직 댓글이 없어요.</p>
                ) : (
                  <ol aria-label="댓글 목록" css={listStyle}>
                    {comments.map((comment) => (
                      <CommentItem key={comment.id} comment={comment} />
                    ))}
                  </ol>
                )}
                {query.isFetchNextPageError && (
                  <p role="alert" css={statusStyle}>
                    댓글을 더 불러오지 못했어요. 다시 시도해주세요.
                  </p>
                )}
                {query.hasNextPage && (
                  <button
                    type="button"
                    css={moreStyle}
                    disabled={query.isFetching}
                    onClick={() => void query.fetchNextPage()}
                  >
                    {query.isFetchingNextPage
                      ? '댓글을 불러오는 중이에요.'
                      : query.isFetchNextPageError
                        ? '댓글 더 보기 다시 시도'
                        : '댓글 10개 더 보기'}
                  </button>
                )}
              </>
            )}
          </div>
        </BottomSheet>,
        document.body,
      )}
    </>
  );
}
