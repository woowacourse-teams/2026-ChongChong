import studyQueries from '../queries';
import { useMemo } from 'react';
import usePageNavigation from '../../../shared/hooks/usePageNavigation';
import { URL_POLICY } from '../../../shared/constants/urlPolicy';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createStudy } from '../api';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

export default function useCreateStudy() {
  const { move } = usePageNavigation();
  const toast = useToast();
  const queryClient = useQueryClient();

  const { mutate, isPending, error } = useMutation({
    mutationFn: createStudy,
    onSuccess: async (data) => {
      await queryClient.invalidateQueries({ queryKey: studyQueries.all() });
      move(URL_POLICY.afterStudyCreate(data.studyId));
    },
    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast message={error.message} status={'error'} />);
    },
  });

  const fieldErrors = useMemo(
    () => (error instanceof ValidationError ? error.fieldErrors : {}),
    [error],
  );

  return { mutate, isPending, fieldErrors };
}
