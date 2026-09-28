import { useLocation } from 'react-router';
import { useSearchParams } from 'react-router';
import { useInputState } from '../../../shared/hooks/useInputState';

export default function useInviteLinkState() {
  function hasTokenInQueryParams() {
    return searchParams.has('token');
  }

  function getCurrentUrl() {
    return new URL(`${location.pathname}${location.search}${location.hash}`, window.location.origin)
      .href;
  }

  const location = useLocation();
  const [searchParams] = useSearchParams();

  const [inviteLink, handleInviteLink] = useInputState(() => {
    if (!hasTokenInQueryParams()) return '';
    return getCurrentUrl();
  });

  return [inviteLink, handleInviteLink] as const;
}
