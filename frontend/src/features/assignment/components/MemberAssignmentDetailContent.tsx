import { useMemo } from 'react';
import { useQueryClient, useSuspenseQueries, useMutation } from '@tanstack/react-query';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import assignmentQueries from '../queries';
import AssignmentArticle from './AssignmentArticle';
import { AssignmentSubmissionValue } from '../types';
import { createAssignmentSubmission } from '../api';
import CompletedAssignmentSubmission from './CompletedAssignmentSubmission';
import AssignmentSubmissionForm from './AssignmentSubmissionForm';
import { useToast } from '../../../shared/providers/ToastProvider';
import { ValidationError } from '../../../shared/api/error';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Props {
  studyId: number;
}

export default function MemberAssignmentDetailContent({ studyId }: Props) {
  const { assignmentId } = useIntegerParams(['assignmentId']);
  const queryClient = useQueryClient();
  const toast = useToast();

  const [{ data: assignment }, { data: submission }] = useSuspenseQueries({
    queries: [
      assignmentQueries.detail(studyId, assignmentId),
      assignmentQueries.mySubmission(studyId, assignmentId),
    ],
  });

  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      createAssignmentSubmission(studyId, assignmentId, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(studyId) });
    },
    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast message={error.message} status={'Error'} />);
    },
  });

  const fieldErrors = useMemo(
    () => (error instanceof ValidationError ? error.fieldErrors : {}),
    [error],
  );

  return (
    <>
      <AssignmentArticle assignment={assignment} />

      {submission.submitted ? (
        <CompletedAssignmentSubmission
          key={`${studyId}-${assignmentId}-${submission.submissionId}`}
          assignmentId={assignmentId}
          studyId={studyId}
          submission={submission}
        />
      ) : (
        <AssignmentSubmissionForm
          key={`${studyId}-${assignmentId}`}
          isSubmitting={isPending}
          onSubmit={mutate}
          fieldErrors={fieldErrors}
        />
      )}
    </>
  );
}
