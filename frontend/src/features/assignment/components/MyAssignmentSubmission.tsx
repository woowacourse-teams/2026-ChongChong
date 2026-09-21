import { useMutation, useQueryClient } from '@tanstack/react-query';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import { createAssignmentSubmission } from '../api';
import assignmentQueries from '../queries';
import type { AssignmentSubmissionValue, UserAssignmentSubmitDetail } from '../types';
import AssignmentSubmissionForm from './AssignmentSubmissionForm';
import CompletedAssignmentSubmission from './CompletedAssignmentSubmission';

interface Props {
  studyId: number;
  assignmentId: number;
  submission: UserAssignmentSubmitDetail;
}

export default function MyAssignmentSubmission({ studyId, assignmentId, submission }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: AssignmentSubmissionValue) =>
      createAssignmentSubmission(studyId, assignmentId, values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(studyId) });
    },
    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast message={error.message} status="Error" />);
    },
  });

  const fieldErrors = error instanceof ValidationError ? error.fieldErrors : {};

  return submission.submitted ? (
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
  );
}
