import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Suspense, useRef } from 'react';
import { useNavigate, useParams } from 'react-router';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import Loading from '../../../shared/ui/Loading';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { deleteAssignment } from '../api';
import LeaderAssignmentDetailContent from '../components/LeaderAssignmentDetailContent';
import assignmentQueries from '../queries';

export default function LeaderAssignmentDetailpage() {
  const navigate = useNavigate();
  const { studyId, assignmentId } = useParams();
  const queryClient = useQueryClient();
  const deleteDialogRef = useRef<HTMLDialogElement>(null);

  const deleteMutation = useMutation({
    mutationFn: () => deleteAssignment(Number(studyId), Number(assignmentId)),
    onSuccess: () => {
      queryClient.removeQueries({
        queryKey: assignmentQueries.detail(Number(studyId), Number(assignmentId)).queryKey,
      });
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(Number(studyId)),
      });
      navigate(`/studies/${studyId}/assignments`);
    },
  });

  const openDeleteDialog = () => deleteDialogRef.current?.showModal();
  const closeDeleteDialog = () => deleteDialogRef.current?.close();
  const editAssignment = () => navigate(`/studies/${studyId}/assignments/${assignmentId}/edit`);

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Main>
        <Suspense fallback={<Loading />}>
          <LeaderAssignmentDetailContent
            studyId={Number(studyId)}
            assignmentId={Number(assignmentId)}
            onEdit={editAssignment}
            onDelete={openDeleteDialog}
          />
        </Suspense>
      </Main>

      <ConfirmDialog
        ref={deleteDialogRef}
        title="과제를 삭제할까요?"
        description={'삭제한 과제는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
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
