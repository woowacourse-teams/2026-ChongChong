import { useMutation, useQueryClient, useSuspenseQueries } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import DetailActions from '../../../shared/widgets/DetailActions';
import { deleteNotice } from '../api';
import noticeQueries from '../queries';
import NoticeArticle from './NoticeArticle';
import NoticeReadStatus from './NoticeReadStatus';
import useDialogControl from '../../../shared/hooks/useDialogControl';

interface Props {
  studyId: number;
  noticeId: number;
}

export default function LeaderNoticeDetailContent({ studyId, noticeId }: Props) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const deleteMutation = useMutation({
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
  });

  const {
    dialogRef: deleteConfirmDialogRef,
    open: openDeleteConfimDialog,
    close: closeDeleteConfirmDialog,
  } = useDialogControl();

  const goToEditNotice = () => navigate(`/studies/${studyId}/notices/${noticeId}/edit`);

  const [{ data: notice }, { data: readStatus }] = useSuspenseQueries({
    queries: [noticeQueries.detail(studyId, noticeId), noticeQueries.readStatus(studyId, noticeId)],
  });

  return (
    <>
      <NoticeReadStatus status={readStatus} />
      <NoticeArticle notice={notice} />
      <div css={{ marginTop: 'auto' }}>
        <DetailActions onEdit={goToEditNotice} onDelete={openDeleteConfimDialog} />
      </div>

      <ConfirmDialog
        ref={deleteConfirmDialogRef}
        title="공지를 삭제할까요?"
        description={'삭제한 공지는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
        closeButton={
          <ConfirmDialog.CloseButton onClick={closeDeleteConfirmDialog}>
            취소
          </ConfirmDialog.CloseButton>
        }
        confirmButton={
          <ConfirmDialog.ConfirmButton
            disabled={deleteMutation.isPending}
            onClick={() => deleteMutation.mutate()}
          >
            {deleteMutation.isPending ? '삭제 중...' : '삭제'}
          </ConfirmDialog.ConfirmButton>
        }
      />
    </>
  );
}
