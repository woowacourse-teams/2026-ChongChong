import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CreateAssignmentFormValues } from '../types';
import { createAssignment } from '../api';
import { useCompleteFunnel } from '../../../shared/providers/FunnelNavigationProvider';
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
  const completeFunnel = useCompleteFunnel();
  const toast = useToast();
  const posthog = usePostHog();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (body: CreateAssignmentFormValues) => createAssignment(studyId, body),
    onSuccess: ({ assignmentId }, body) => {
      queryClient.invalidateQueries({
        queryKey: assignmentQueries.lists(studyId),
      });
      posthog?.capture('assignment_saved', {
        content_type: 'assignment',
        mode: 'create',
        attachment_count: body.attachmentFileIds?.length ?? 0,
      });

      completeFunnel(URL_POLICY.afterAssignmentCreate(studyId, assignmentId));
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
