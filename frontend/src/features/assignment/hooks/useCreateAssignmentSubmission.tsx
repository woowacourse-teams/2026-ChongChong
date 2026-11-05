import { useMutation, useQueryClient } from '@tanstack/react-query';
import { usePostHog } from '@posthog/react';
import { AssignmentSubmissionFormValues } from '../types';
import { createAssignmentSubmission } from '../api';
import assignmentQueries from '../queries';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Params {
  studyId: number;
  assignmentId: number;
}

export default function useCreateAssignmentSubmission({ studyId, assignmentId }: Params) {
  const queryClient = useQueryClient();
  const posthog = usePostHog();
  const toast = useToast();
  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: AssignmentSubmissionFormValues) =>
      createAssignmentSubmission(studyId, assignmentId, values),
    onSuccess: async (_data, values) => {
      posthog?.capture('assignment_submission_saved', {
        content_type: 'submission',
        mode: 'create',
        attachment_count: values.attachmentFileIds?.length ?? 0,
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: assignmentQueries.lists(studyId) }),
        queryClient.invalidateQueries({
          queryKey: assignmentQueries.mySubmission(studyId, assignmentId).queryKey,
        }),
      ]);
    },
    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast message={error.message} status="error" />);
    },
  });

  const fieldErrors = error instanceof ValidationError ? error.fieldErrors : {};

  return { mutate, isPending, fieldErrors };
}
