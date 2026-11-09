import { useId, useRef, useState, type CSSProperties } from 'react';
import { useSuspenseInfiniteQuery, useIsMutating } from '@tanstack/react-query';
import commentQueries from '../../modules/comments/queries';
import type { CommentTarget } from '../../modules/comments/types';
import BottomSheet from '../../ui/BottomSheet';
import { tokens, typography } from '../../../styles/global';
import CommentItem from './CommentItem';
import closeIcon from '../../assets/close.svg';
import List from '../../ui/List';
import CommentEditor from './CommentEditor';
import type { CSSObject } from '@emotion/react';
import { commentScrollbarStyle } from './scrollStyle';
import CommentDeleteDialog from './CommentDeleteDialog';
import type { Comment } from '../../modules/comments/responseSchemas';
import CommentsQueryBoundary from './CommentsQueryBoundary';
import CommentsLoadMore from './CommentsLoadMore';

type PanelState = { kind: 'closed' | 'list' } | { kind: 'edit' | 'delete'; comment: Comment };

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
  cursor: 'pointer',
} satisfies CSSProperties;

const scrollStyle = {
  ...commentScrollbarStyle,
  minHeight: 0,
  overflowY: 'auto',
  overscrollBehavior: 'contain',
  ...typography.body,
} satisfies CSSObject;

const emptyStyle = {
  paddingBlock: tokens.spacing[4],
  color: tokens.text.secondary,
  ...typography.body,
} satisfies CSSProperties;

export default function CommentsPanel(props: Props) {
  return (
    <CommentsQueryBoundary>
      <PanelContent {...props} />
    </CommentsQueryBoundary>
  );
}

function PanelContent({ target, title }: Props) {
  const [view, setView] = useState<PanelState>({ kind: 'closed' });
  const [createFormKey, setCreateFormKey] = useState(0);
  const titleId = useId();
  const closeRef = useRef<HTMLButtonElement>(null);
  const writesPending = useIsMutating({ mutationKey: commentQueries.target(target) }) > 0;
  const { data, hasNextPage, fetchNextPage } = useSuspenseInfiniteQuery(
    commentQueries.list(target),
  );
  const label = `${title} ${data.totalCount}`;

  function returnToList() {
    const commentId = 'comment' in view ? view.comment.id : null;
    setView({ kind: 'list' });
    requestAnimationFrame(() => {
      const button =
        commentId === null
          ? null
          : document.getElementById(`${titleId}-comment-${commentId}`)?.querySelector('button');
      (button ?? closeRef.current)?.focus();
    });
  }
  function finishDeletion() {
    setView({ kind: 'list' });
    requestAnimationFrame(() => closeRef.current?.focus());
  }

  function closePanel() {
    if (writesPending) return;
    setView({ kind: 'closed' });
  }

  return (
    <>
      <button
        type="button"
        css={entryStyle}
        aria-label={`${label} 열기`}
        aria-haspopup="dialog"
        aria-expanded={view.kind !== 'closed'}
        onClick={() => setView({ kind: 'list' })}
      >
        <span>댓글 {data.totalCount}</span>
        <span css={{ ...typography.footnote, color: tokens.text.secondary, cursor: 'pointer' }}>
          댓글 남기기 ›
        </span>
      </button>

      <BottomSheet
        open={view.kind === 'list' || view.kind === 'edit'}
        onClose={closePanel}
        aria-labelledby={titleId}
        variant="compact"
        maxHeight="50dvh"
      >
        <header css={headerStyle}>
          <h2 id={titleId} css={titleStyle}>
            {view.kind === 'edit' ? '댓글 수정' : label}
          </h2>
          <button
            type="button"
            aria-label="댓글 닫기"
            ref={closeRef}
            disabled={writesPending}
            onClick={closePanel}
            css={closeStyle}
          >
            <img src={closeIcon} alt="" />
          </button>
        </header>
        <div
          hidden={view.kind === 'edit' || view.kind === 'delete'}
          inert={view.kind === 'edit' || view.kind === 'delete'}
          css={{
            display: view.kind === 'closed' || view.kind === 'list' ? 'flex' : 'none',
            flexDirection: 'column',
            gap: tokens.spacing[4],
            minHeight: 0,
          }}
        >
          <div css={scrollStyle}>
            {data.comments.length === 0 ? (
              <p css={emptyStyle}>아직 댓글이 없어요.</p>
            ) : (
              <List aria-label="댓글 목록">
                {data.comments.map((comment) => (
                  <CommentItem
                    key={comment.id}
                    comment={comment}
                    id={`${titleId}-comment-${comment.id}`}
                    onEdit={(comment) => setView({ kind: 'edit', comment })}
                    onDelete={(comment) => setView({ kind: 'delete', comment })}
                  />
                ))}
              </List>
            )}
            {hasNextPage && (
              <CommentsLoadMore
                onLoad={() => fetchNextPage({ cancelRefetch: false, throwOnError: true })}
              />
            )}
          </div>
          <CommentEditor
            key={createFormKey}
            mode="create"
            target={target}
            onCreated={() => setCreateFormKey((key) => key + 1)}
          />
        </div>
        {view.kind === 'edit' && (
          <CommentEditor
            key={view.comment.id}
            mode="edit"
            target={target}
            comment={view.comment}
            onCancel={returnToList}
            onSaved={returnToList}
          />
        )}
      </BottomSheet>

      {view.kind === 'delete' && (
        <CommentDeleteDialog
          target={target}
          commentId={view.comment.id}
          onClose={returnToList}
          onDeleted={finishDeletion}
        />
      )}
    </>
  );
}
