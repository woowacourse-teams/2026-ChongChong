import { useId, type CSSProperties, type SubmitEvent } from 'react';
import type { CSSObject } from '@emotion/react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createComment, updateComment } from '../../modules/comments/api';
import commentQueries from '../../modules/comments/queries';
import type { Comment } from '../../modules/comments/responseSchemas';
import type { CommentTarget } from '../../modules/comments/types';
import TextArea from '../../ui/inputs/TextArea';
import { tokens, typography } from '../../../styles/global';
import CommentItem from './CommentItem';
import { commentScrollbarStyle } from './scrollStyle';
import { useInputState } from '../../hooks/useInputState';
import List from '../../ui/List';

type Props = { target: CommentTarget } & (
  | { mode: 'create'; onCreated: () => void }
  | { mode: 'edit'; comment: Comment; onCancel: () => void; onSaved: () => void }
);

const formStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[4],
  minHeight: 0,
} satisfies CSSProperties;

const scrollStyle = {
  ...commentScrollbarStyle,
  minHeight: 0,
  overflowY: 'auto',
  overscrollBehavior: 'contain',
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[4],
} satisfies CSSObject;

const fieldStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[1],
  flexShrink: 0,
} satisfies CSSProperties;

const inputRowStyle = {
  display: 'flex',
  alignItems: 'flex-end',
  border: tokens.border.default,
  borderRadius: tokens.radius.md,
  paddingInline: tokens.spacing[4],
  '&:focus-within': { border: tokens.border.brand },
} satisfies CSSObject;

const textAreaStyle = {
  ...commentScrollbarStyle,
  border: 0,
  borderRadius: tokens.radius.md,
  padding: '14px 0',
  height: '52px',
  minHeight: '52px',
  ...typography.subtitle,
  color: tokens.text.primary,
  background: tokens.bg.default,
  maxHeight: '96px',
  flex: 1,
  minWidth: 0,
  '&:focus': { border: 0, outline: 'none' },
} satisfies CSSObject;

const submitStyle = {
  ...typography.footnote,
  color: tokens.text.brand,
  padding: '17px 0 17px 12px',
  flexShrink: 0,
} satisfies CSSProperties;

const hintStyle = {
  margin: 0,
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

const actionRowStyle = {
  display: 'flex',
  gap: tokens.spacing[2],
  flexShrink: 0,
} satisfies CSSProperties;

const actionStyle = {
  ...typography.button,
  display: 'grid',
  placeItems: 'center',
  minHeight: '52px',
  flex: 1,
  borderRadius: tokens.radius.md,
  color: tokens.text.primary,
  border: tokens.border.default,
} satisfies CSSProperties;

export default function CommentEditor(props: Props) {
  const [content, handleContentChange] = useInputState(
    props.mode === 'edit' ? props.comment.content : '',
    (value, prevValue) => {
      if (value.trim().length > 1000) return prevValue;
      return value;
    },
  );
  const queryClient = useQueryClient();
  const id = useId();
  const normalized = content.trim();
  const mutation = useMutation({
    mutationKey: commentQueries.target(props.target),
    mutationFn: async (value: string) => {
      if (props.mode === 'create') {
        await createComment(props.target, value);
      } else {
        await updateComment(props.target, props.comment.id, value);
      }
    },
    retry: false,
    onSuccess: async () => {
      if (props.mode === 'create') {
        props.onCreated();
        // 요청 성공 이후 조회 실패를 등록 실패로 바꾸지 않는다.
        await queryClient.invalidateQueries({ queryKey: commentQueries.target(props.target) });
      } else {
        props.onSaved();
        await queryClient.invalidateQueries({ queryKey: commentQueries.target(props.target) });
      }
    },
  });

  function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!normalized || mutation.isPending || (props.mode === 'edit' && !props.comment.canEdit))
      return;
    mutation.mutate(normalized);
  }

  return (
    <form
      aria-label={props.mode === 'edit' ? '댓글 수정' : '댓글 작성'}
      onSubmit={submit}
      css={[formStyle, props.mode === 'create' && { flexShrink: 0 }]}
    >
      <div css={props.mode === 'edit' ? scrollStyle : undefined}>
        {props.mode === 'edit' && (
          <List aria-label="수정할 댓글">
            <CommentItem comment={props.comment} />
          </List>
        )}
        <div css={fieldStyle}>
          <div css={inputRowStyle}>
            <TextArea
              aria-label={props.mode === 'edit' ? '댓글 수정 내용' : '댓글 내용'}
              aria-describedby={`${id}-hint`}
              placeholder="댓글을 입력해주세요"
              value={content}
              readOnly={mutation.isPending}
              onChange={(event) => {
                handleContentChange(event);
                mutation.reset();
              }}
              css={[textAreaStyle, props.mode === 'edit' && { height: '62px', minHeight: '62px' }]}
            />
            {props.mode === 'create' && (
              <button
                type="submit"
                disabled={!normalized || mutation.isPending}
                css={[
                  submitStyle,
                  (!normalized || mutation.isPending) && { color: tokens.text.placeholder },
                ]}
              >
                {mutation.isPending ? '등록 중' : '등록'}
              </button>
            )}
          </div>
          <p id={`${id}-hint`} css={[hintStyle, { textAlign: 'right' }]}>
            {`${normalized.length} / 1,000`}
          </p>
          {mutation.error && (
            <p role="alert" css={[hintStyle, { color: tokens.text.critical }]}>
              {mutation.error.message}
            </p>
          )}
        </div>
      </div>
      {props.mode === 'edit' && (
        <div css={actionRowStyle}>
          <button
            type="button"
            disabled={mutation.isPending}
            onClick={props.onCancel}
            css={[
              actionStyle,
              { background: tokens.color.optionSub6, border: 0, color: tokens.text.secondary },
            ]}
          >
            취소
          </button>
          <button
            type="submit"
            disabled={!normalized || mutation.isPending || !props.comment.canEdit}
            css={[
              actionStyle,
              (!normalized || mutation.isPending) && { color: tokens.text.placeholder },
            ]}
          >
            {mutation.isPending ? '저장 중' : '저장'}
          </button>
        </div>
      )}
    </form>
  );
}
