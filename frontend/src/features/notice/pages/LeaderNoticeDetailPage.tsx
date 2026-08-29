import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Suspense, useRef } from 'react';
import { useNavigate, useParams } from 'react-router';
import BottomTab from '../../../shared/widgets/BottomTab';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import Loading from '../../../shared/ui/Loading';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { deleteNotice } from '../api';
import LeaderNoticeDetailContent from '../components/LeaderNoticeDetailContent';
import noticeQueries from '../queries';

export default function LeaderNoticeDetailPage() {
  const navigate = useNavigate();
  const { studyId, noticeId } = useParams();
  const queryClient = useQueryClient();
  const deleteDialogRef = useRef<HTMLDialogElement>(null);
  const numericStudyId = Number(studyId);
  const numericNoticeId = Number(noticeId);

  const deleteMutation = useMutation({
    mutationFn: () => deleteNotice(numericStudyId, numericNoticeId),
    onSuccess: () => {
      queryClient.removeQueries({
        queryKey: noticeQueries.detail(numericStudyId, numericNoticeId).queryKey,
      });
      queryClient.removeQueries({
        queryKey: noticeQueries.readStatus(numericStudyId, numericNoticeId).queryKey,
      });
      queryClient.invalidateQueries({ queryKey: noticeQueries.lists(numericStudyId) });
      navigate(`/studies/${studyId}/notices`);
    },
  });

  const openDeleteDialog = () => deleteDialogRef.current?.showModal();
  const closeDeleteDialog = () => deleteDialogRef.current?.close();
  const editNotice = () => navigate(`/studies/${studyId}/notices/${noticeId}/edit`);

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>공지</TopHeader.Title>} />
      <Main>
        <Suspense fallback={<Loading />}>
          <LeaderNoticeDetailContent
            studyId={numericStudyId}
            noticeId={numericNoticeId}
            onEdit={editNotice}
            onDelete={openDeleteDialog}
          />
        </Suspense>
      </Main>

      <ConfirmDialog
        ref={deleteDialogRef}
        title="공지를 삭제할까요?"
        description={'삭제한 공지는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
        closeButton={
          <ConfirmDialog.CloseButton onClick={closeDeleteDialog}>취소</ConfirmDialog.CloseButton>
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
      <BottomTab />
    </Page>
  );
}
