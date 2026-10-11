import { useMemo } from 'react';
import { useNavigate } from 'react-router';
import { useMutation } from '@tanstack/react-query';
import { createStudy } from '../api';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

export default function useCreateStudy() {
  const navigate = useNavigate();
  const toast = useToast();

  const { mutate, isPending, error } = useMutation({
    mutationFn: createStudy,
    onSuccess: (data) => navigate(`/studies/${data.studyId}`),
    onError: (error) => {
      if (error instanceof ValidationError) return;
      toast.open(<StatusToast message={error.message} status={'Error'} />);
    },
  });

  const fieldErrors = useMemo(
    () => (error instanceof ValidationError ? error.fieldErrors : {}),
    [error],
  );

  return { mutate, isPending, fieldErrors };
}
