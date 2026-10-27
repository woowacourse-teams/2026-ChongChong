import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateAssignment } from '../api';
import { UpdateAssignmentFormValues } from '../types';
import { useToast } from '../../../shared/providers/ToastProvider';
import { usePostHog } from '@posthog/react';
import { useNavigate } from 'react-router';
import assignmentQueries from '../queries';
import { ValidationError } from '../../../shared/api/error';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

interface Params {
  studyId: number;
  assignmentId: number;
}

export default function useEditAssignment({ studyId, assignmentId }: Params) {
  const toast = useToast();
  const posthog = usePostHog();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: UpdateAssignmentFormValues) =>
      updateAssignment(studyId, assignmentId, values),
    onSuccess: (_data, values) => {
      posthog?.capture('assignment_saved', {
        content_type: 'assignment',
        mode: 'edit',
        attachment_count: values.attachmentFileIds?.length ?? 0,
      });
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(studyId),
      });

      queryClient.invalidateQueries({
        queryKey: assignmentQueries.detail(studyId, assignmentId).queryKey,
      });

      navigate(`/studies/${studyId}/assignments/${assignmentId}`);
    },

    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast status="error" message={error.message} />);
    },
  });

  const fieldErrors = useMemo(() => {
    return error instanceof ValidationError ? error.fieldErrors : {};
  }, [error]);

  return { mutate, isPending, fieldErrors };
}
