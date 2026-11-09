import { useMutation, useQueryClient } from '@tanstack/react-query';
import { deleteComment } from '../../modules/comments/api';
import commentQueries from '../../modules/comments/queries';
import type { CommentTarget } from '../../modules/comments/types';
import ConfirmDialog from '../../ui/dialogs/ConfirmDialog';

interface Props {
  target: CommentTarget;
  commentId: number;
  onClose: () => void;
  onDeleted: () => void;
}
export default function CommentDeleteDialog({ target, commentId, onClose, onDeleted }: Props) {
  const queryClient = useQueryClient();
  const mutation = useMutation({
    mutationKey: commentQueries.target(target),
    mutationFn: () => deleteComment(target, commentId),
    retry: false,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: commentQueries.target(target) });
      onDeleted();
    },
  });
  return (
    <ConfirmDialog
      title="댓글을 삭제할까요?"
      description={`삭제한 댓글은 다시 복구할 수 없어요.\n정말 삭제하시겠어요?`}
      onClose={onClose}
      closeDisabled={mutation.isPending}
      closeButton={
        <ConfirmDialog.CloseButton disabled={mutation.isPending} onClick={onClose}>
          취소
        </ConfirmDialog.CloseButton>
      }
      confirmButton={
        <ConfirmDialog.ConfirmButton
          disabled={mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          {mutation.isPending ? '삭제 중...' : '삭제'}
        </ConfirmDialog.ConfirmButton>
      }
    />
  );
}
