import { useState, type ChangeEventHandler } from 'react';
import { useLocation } from 'react-router';

const INVITE_LINK_DRAFT_KEY = 'study_join_invite_link';

export function clearInviteLinkDraft() {
  sessionStorage.removeItem(INVITE_LINK_DRAFT_KEY);
}

export default function useInviteLinkState() {
  const location = useLocation();
  const currentPath = `${location.pathname}${location.search}${location.hash}`;

  const [inviteLink, setInviteLink] = useState(() => {
    const saved = sessionStorage.getItem(INVITE_LINK_DRAFT_KEY);
    if (saved) {
      try {
        const draft: { path: string; value: unknown } = JSON.parse(saved);
        // 다른 초대 링크로 새로 진입하면 이전에 작성한 값으로 덮어쓰지 않는다.
        if (draft.path === currentPath && typeof draft.value === 'string') return draft.value;
      } catch {
        clearInviteLinkDraft();
      }
    }

    return new URLSearchParams(location.search).has('token')
      ? new URL(currentPath, window.location.origin).href
      : '';
  });

  const handleInviteLink: ChangeEventHandler<HTMLInputElement> = (event) => {
    const value = event.target.value;
    setInviteLink(value);
    sessionStorage.setItem(INVITE_LINK_DRAFT_KEY, JSON.stringify({ path: currentPath, value }));
  };

  return [inviteLink, handleInviteLink] as const;
}
