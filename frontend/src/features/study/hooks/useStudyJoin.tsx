import { clearInviteLinkDraft } from './useInviteLinkState';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { useMemo } from 'react';
import usePageNavigation from '../../../shared/hooks/usePageNavigation';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { joinStudy } from '../api';
import studyQueries from '../queries';
import { ValidationError } from '../../../shared/api/error';
import { useToast } from '../../../shared/providers/ToastProvider';
import StatusToast from '../../../shared/ui/toasts/StatusToast';

export default function useStudyJoin() {
  const { move } = usePageNavigation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { mutate, isPending, error } = useMutation({
    mutationFn: joinStudy,
    onSuccess: async (data) => {
      await queryClient.invalidateQueries({ queryKey: studyQueries.lists() });
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
