import type { StudyFormValues } from '../types';
import studyQueries from '../queries';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { editStudy } from '../api';
import usePageNavigation from '../../../shared/hooks/usePageNavigation';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

export default function useEditStudy(studyId: number) {
  const { move } = usePageNavigation();
  const toast = useToast();
  const queryClient = useQueryClient();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (body: StudyFormValues) => editStudy(studyId, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: studyQueries.all() });
      move(URL_POLICY.afterStudyEdit(studyId));
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
