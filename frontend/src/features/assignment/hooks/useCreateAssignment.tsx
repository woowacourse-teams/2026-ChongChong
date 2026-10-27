import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { AssignmentFormValues } from '../types';
import { createAssignment } from '../api';
import { useNavigate } from 'react-router';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import { usePostHog } from '@posthog/react';
import StatusToast from '../../../shared/ui/toasts/StatusToast';
import assignmentQueries from '../queries';

interface Params {
  studyId: number;
}

export default function useCreateAssignment({ studyId }: Params) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const toast = useToast();
  const posthog = usePostHog();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (body: AssignmentFormValues) => createAssignment(studyId, body),
    onSuccess: ({ assignmentId }, body) => {
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(studyId),
      });
      posthog?.capture('assignment_saved', {
        content_type: 'assignment',
        mode: 'create',
        attachment_count: body.attachmentFileIds?.length ?? 0,
      });

      navigate(`/studies/${studyId}/assignments/${assignmentId}`);
    },
    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast status={'error'} message={error.message} />);
    },
  });

  const fieldErrors = useMemo(() => {
    return error instanceof ValidationError ? error.fieldErrors : {};
  }, [error]);

  return { mutate, isPending, fieldErrors };
}
