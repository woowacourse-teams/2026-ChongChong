import { useQueryClient, useSuspenseQueries, useMutation } from '@tanstack/react-query';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import assignmentQueries from '../queries';
import AssignmentArticle from './AssignmentArticle';
import { AssignmentSubmissionValue } from '../types';
import { createAssignmentSubmission } from '../api';
import CompletedAssignmentSubmission from './CompletedAssignmentSubmission';
import AssignmentSubmissionForm from './AssignmentSubmissionForm';

interface Props {
  studyId: number;
}

export default function MemberAssignmentDetailContent({ studyId }: Props) {
  const { assignmentId } = useIntegerParams(['assignmentId']);
  const queryClient = useQueryClient();

  const [{ data: assignment }, { data: submission }] = useSuspenseQueries({
    queries: [
      assignmentQueries.detail(Number(studyId), Number(assignmentId)),
      assignmentQueries.mySubmission(Number(studyId), Number(assignmentId)),
    ],
  });

  const createMutation = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      createAssignmentSubmission(Number(studyId), Number(assignmentId), values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(Number(studyId)) });
    },
  });

  return (
    <>
      <AssignmentArticle assignment={assignment} />

      {submission.submitted ? (
        <CompletedAssignmentSubmission
          key={`${Number(studyId)}-${Number(assignmentId)}-${submission.submissionId}`}
          assignmentId={Number(assignmentId)}
          studyId={Number(studyId)}
          submission={submission}
        />
      ) : (
        <AssignmentSubmissionForm
          key={`${Number(studyId)}-${Number(assignmentId)}`}
          isSubmitting={createMutation.isPending}
          onSubmit={createMutation.mutate}
        />
      )}
    </>
  );
}
