import { useMutation, useQueryClient, useSuspenseQuery } from '@tanstack/react-query';
import { useParams } from 'react-router';
import { createAssignmentSubmission } from '../api';
import assignmentQueries from '../queries';
import type { AssignmentSubmissionValue } from '../types';
import AssignmentArticle from './AssignmentArticle';
import AssignmentSubmissionForm from './AssignmentSubmissionForm';
import CompletedAssignmentSubmission from './CompletedAssignmentSubmission';

export default function MemberAssignmentDetailContent() {
  const { studyId, assignmentId } = useParams();
  const queryClient = useQueryClient();

  const { data: assignment } = useSuspenseQuery(
    assignmentQueries.detail(Number(studyId), Number(assignmentId)),
  );

  const createMutation = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      createAssignmentSubmission(Number(studyId), Number(assignmentId), values),
    onSuccess: ({ submissionId }) => {
      queryClient.setQueryData(
        assignmentQueries.detail(Number(studyId), Number(assignmentId)).queryKey,
        { ...assignment, submissionId },
      );
      queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(Number(studyId)) });
    },
  });

  return (
    <>
      <AssignmentArticle assignment={assignment} />

      {assignment.submissionId ? (
        <CompletedAssignmentSubmission
          key={`${Number(studyId)}-${Number(assignmentId)}-${assignment.submissionId}`}
          assignmentId={Number(assignmentId)}
          studyId={Number(studyId)}
          submissionId={assignment.submissionId}
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
