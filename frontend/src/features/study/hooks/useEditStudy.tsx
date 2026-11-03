import type { StudyFormValues } from '../types';
import studyQueries from '../queries';
import { useMemo } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { editStudy } from '../api';
import { useNavigate } from 'react-router';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

export default function useEditStudy(studyId: number) {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (body: StudyFormValues) => editStudy(studyId, body),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: studyQueries.all() });
      navigate(`/studies/${studyId}`);
    },
    onError: (error) => {
      if (error instanceof ValidationError) {
        if (error.fieldErrors.imageFileId)
          toast.open(<StatusToast message={error.fieldErrors.imageFileId} status="error" />);
        return;
      }
      toast.open(<StatusToast message={error.message} status={'error'} />);
    },
  });

  const fieldErrors = useMemo(
    () => (error instanceof ValidationError ? error.fieldErrors : {}),
    [error],
  );

  return { mutate, isPending, fieldErrors };
}
