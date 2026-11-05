import calendarQueries from '../../calendar/queries';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateAssignment } from '../api';
import { UpdateAssignmentFormValues } from '../types';
import { useToast } from '../../../shared/providers/ToastProvider';
import { usePostHog } from '@posthog/react';
import usePageNavigation from '../../../shared/hooks/usePageNavigation';
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
  const { move } = usePageNavigation();
  const { mutate, isPending, error } = useMutation({
    mutationFn: (values: UpdateAssignmentFormValues) =>
      updateAssignment(studyId, assignmentId, values),
    onSuccess: async (_data, values) => {
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

      queryClient.invalidateQueries({ queryKey: calendarQueries.months(studyId) });

      await move(URL_POLICY.afterAssignmentEdit(studyId, assignmentId));
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
