import { useMemo } from 'react';
import { useMutation } from '@tanstack/react-query';
import { editStudy } from '../api';
import { useNavigate } from 'react-router';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

export default function useEditStudy(studyId: number) {
  const navigate = useNavigate();
  const toast = useToast();

  const { mutate, isPending, error } = useMutation({
    mutationFn: (body: { name: string; description: string }) => editStudy(studyId, body),
    onSuccess: () => {
      navigate(`/studies/${studyId}`);
    },
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
