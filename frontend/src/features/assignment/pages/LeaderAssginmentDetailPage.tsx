import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import { useNavigate, useParams } from 'react-router';
import { useRef, Suspense } from 'react';
import DetailActions from '../../../shared/ui/components/DetailActions';
import Main from '../../../shared/ui/Main';
import assignmentQueries from '../queries';
import { useSuspenseQueries, useMutation, useQueryClient } from '@tanstack/react-query';
import SubmitStatusSection from '../components/SubmitStatusSection';
import AssignmentArticle from '../components/AssignmentArticle';
import SubmissionList from '../components/SubmissionList';
import { deleteAssignment } from '../api';
import Loading from '../../../shared/ui/Loading';

export default function LeaderAssignmentDetailpage() {
  const navigate = useNavigate();
  const { studyId, assignmentId } = useParams();
  const queryClient = useQueryClient();
  const deleteDialogRef = useRef<HTMLDialogElement>(null);

  const [{ data: assignment }, { data: submitStatusResponse }, { data: submissions }] =
    useSuspenseQueries({
      queries: [
        assignmentQueries.detail(Number(studyId), Number(assignmentId)),
        assignmentQueries.submitStatus(Number(studyId), Number(assignmentId)),
        assignmentQueries.submissions(Number(studyId), Number(assignmentId)),
      ],
    });

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
      <Suspense fallback={<Loading />}>
        <Main>
          <SubmitStatusSection status={submitStatusResponse} />
          <AssignmentArticle assignment={assignment} />
          <SubmissionList submissions={submissions.submissions} />

          <DetailActions onEdit={editAssignment} onDelete={openDeleteDialog} />
        </Main>
      </Suspense>

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
