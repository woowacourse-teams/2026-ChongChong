import { useMutation, useQueryClient, useSuspenseQueries } from '@tanstack/react-query';
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
