import { clearInviteLinkDraft } from './useInviteLinkState';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { useMemo } from 'react';
import usePageNavigation from '../../../shared/hooks/usePageNavigation';
import { useMutation } from '@tanstack/react-query';
import { joinStudy } from '../api';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

export default function useStudyJoin() {
  const { move } = usePageNavigation();
  const toast = useToast();
  const { mutate, isPending, error } = useMutation({
    mutationFn: ({ token }: { token: string }) => joinStudy({ token }),
    onSuccess: (data) => {
      clearInviteLinkDraft();
      return move(URL_POLICY.afterStudyJoin(data.studyId));
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
