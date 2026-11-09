import usePageNavigation from '../../../shared/hooks/usePageNavigation';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import studyQueries from '../queries';
import { ValidationError } from '../../../shared/api/error';

function extractInviteToken(inviteLink: string) {
  try {
    const url = new URL(inviteLink, window.location.origin);
    const tokenParam = url.searchParams.get('token');
    return tokenParam;
  } catch {
    return null;
  }
}

export default function useValidateStudyInvite() {
  const { move } = usePageNavigation();

  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  async function validateInvite(inviteLink: string) {
    if (isPending) return;
    setErrorMessage('');

    const token = extractInviteToken(inviteLink);

    if (!token?.trim()) {
      setErrorMessage('유효한 초대 링크를 입력해 주세요.');
      return;
    }

    setIsPending(true);
    try {
      await queryClient.fetchQuery(studyQueries.joinInfo(token));
      move(URL_POLICY.toStudyJoinInfo(token));
    } catch (error) {
      setErrorMessage(
        error instanceof ValidationError
          ? (error.fieldErrors.token ?? error.message)
          : error instanceof Error
            ? error.message
            : '스터디 참여 정보를 불러오는데 실패했습니다.',
      );
    } finally {
      setIsPending(false);
    }
  }

  function clearError() {
    setErrorMessage('');
  }

  return { validateInvite, isPending, errorMessage, clearError };
}
