import { useMutation, useQueryClient, useSuspenseQueries } from '@tanstack/react-query';
import { useNavigate } from 'react-router';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import ConfirmDialog from '../../../shared/ui/dialogs/ConfirmDialog';
import { deleteAssignment } from '../api';
import useDialogControl from '../../../shared/hooks/useDialogControl';
import SubmitStatusSection from './SubmitStatusSection';
import AssignmentArticle from './AssignmentArticle';
import SubmissionList from './SubmissionList';
import DetailActions from '../../../shared/widgets/DetailActions';
import assignmentQueries from '../queries';

interface Props {
  studyId: number;
}

export default function LeaderAssignmentDetailContent({ studyId }: Props) {
  const navigate = useNavigate();
  const { assignmentId } = useIntegerParams(['assignmentId']);
  const queryClient = useQueryClient();
  const {
    dialogRef: deleteConfirmDialog,
    open: openDeleteConfirmDialog,
    close: closeDeleteConfirmDialog,
  } = useDialogControl();

  const deleteMutation = useMutation({
    mutationFn: () => deleteAssignment(studyId, assignmentId),
    onSuccess: () => {
      queryClient.removeQueries({
        queryKey: assignmentQueries.detail(studyId, assignmentId).queryKey,
      });
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(studyId),
      });
      navigate(`/studies/${studyId}/assignments`);
    },
  });

  const [{ data: assignment }, { data: submitStatusResponse }, { data: submissions }] =
    useSuspenseQueries({
      queries: [
        assignmentQueries.detail(studyId, assignmentId),
        assignmentQueries.submitStatus(studyId, assignmentId),
        assignmentQueries.submissions(studyId, assignmentId),
      ],
    });

  const handleEditAssignment = () =>
    navigate(`/studies/${studyId}/assignments/${assignmentId}/edit`);

  return (
    <>
      <SubmitStatusSection status={submitStatusResponse} />
      <AssignmentArticle assignment={assignment} />
      <SubmissionList submissions={submissions.submissions} />

      <DetailActions onEdit={handleEditAssignment} onDelete={openDeleteConfirmDialog} />
      <ConfirmDialog
        ref={deleteConfirmDialog}
        title="과제를 삭제할까요?"
        description={'삭제한 과제는 다시 복구할 수 없어요.\n정말 삭제하시겠어요?'}
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
