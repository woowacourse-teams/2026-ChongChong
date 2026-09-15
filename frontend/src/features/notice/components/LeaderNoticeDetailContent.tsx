import { useMutation, useQueryClient, useSuspenseQueries } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import DetailActions from '../../../shared/widgets/DetailActions';
import { deleteNotice } from '../api';
import noticeQueries from '../queries';
import NoticeArticle from './NoticeArticle';
import NoticeReadStatus from './NoticeReadStatus';
import useBooleanState from '../../../shared/hooks/useBooleanState';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Props {
  studyId: number;
  noticeId: number;
}

export default function LeaderNoticeDetailContent({ studyId, noticeId }: Props) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [isOpen, openDialog, closeDialog] = useBooleanState();

  const { mutate, isPending } = useMutation({
    mutationFn: () => deleteNotice(studyId, noticeId),
    onSuccess: () => {
      queryClient.removeQueries({
        queryKey: noticeQueries.detail(studyId, noticeId).queryKey,
      });
      queryClient.removeQueries({
        queryKey: noticeQueries.readStatus(studyId, noticeId).queryKey,
      });
      queryClient.invalidateQueries({ queryKey: noticeQueries.lists(studyId) });
      navigate(`/studies/${studyId}/notices`);
    },
    onError: (error) => {
      closeDialog();
      toast.open(<StatusToast status="Error" message={error.message} />);
    },
  });

  const goToEditNotice = () => navigate(`/studies/${studyId}/notices/${noticeId}/edit`);

  const [{ data: notice }, { data: readStatus }] = useSuspenseQueries({
    queries: [noticeQueries.detail(studyId, noticeId), noticeQueries.readStatus(studyId, noticeId)],
  });

  return (
    <>
      <NoticeReadStatus status={readStatus} />
      <NoticeArticle notice={notice} />
      <div css={{ marginTop: 'auto' }}>
        <DetailActions onEdit={goToEditNotice} onDelete={openDialog} />
      </div>

      {isOpen && (
        <ConfirmDialog
          title="공지를 삭제할까요?"
          description={'삭제한 공지는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
          onClose={closeDialog}
          closeButton={
            <ConfirmDialog.CloseButton onClick={closeDialog}>취소</ConfirmDialog.CloseButton>
          }
          confirmButton={
            <ConfirmDialog.ConfirmButton disabled={isPending} onClick={() => mutate()}>
              {isPending ? '삭제 중...' : '삭제'}
            </ConfirmDialog.ConfirmButton>
          }
        />
      )}
    </>
  );
}
