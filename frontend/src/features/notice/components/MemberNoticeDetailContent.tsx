import { useMutation, useQueryClient, useSuspenseQueries } from '@tanstack/react-query';
import type { CSSProperties, UIEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { formatRelativeTime } from '../../../shared/utils/formatDate';
import Main from '../../../shared/ui/Main';
import { tokens } from '../../../styles/global';
import { updateNoticeRead } from '../api';
import noticeQueries from '../queries';
import MemberNoticeReadState from './MemberNoticeReadState';
import NoticeArticle from './NoticeArticle';

interface Props {
  studyId: number;
  noticeId: number;
}

const contentStyle = {
  width: '100%',
  minHeight: 0,
  margin: '0 auto',
  padding: `${tokens.spacing[5]} ${tokens.layout.gutter} ${tokens.spacing[8]}`,
  overflowY: 'auto',
} satisfies CSSProperties;

export default function MemberNoticeDetailContent({ studyId, noticeId }: Props) {
  const queryClient = useQueryClient();
  const contentRef = useRef<HTMLElement>(null);
  const [{ data: notice }, { data: readStatus }] = useSuspenseQueries({
    queries: [noticeQueries.detail(studyId, noticeId), noticeQueries.myRead(studyId, noticeId)],
  });
  const hasRequestedReadRef = useRef(readStatus.isRead);
  const [readProgress, setReadProgress] = useState(() => (readStatus.isRead ? 100 : 0));

  const updateReadMutation = useMutation({
    mutationFn: () => updateNoticeRead(studyId, noticeId),
    onSuccess: (updatedReadStatus) => {
      queryClient.setQueryData(noticeQueries.myRead(studyId, noticeId).queryKey, {
        isRead: true,
        readAt: updatedReadStatus.readAt,
      });
      queryClient.invalidateQueries({ queryKey: noticeQueries.lists(studyId) });
    },
    onError: () => {
      hasRequestedReadRef.current = false;
    },
  });
  const markAsRead = updateReadMutation.mutate;

  const requestMarkAsRead = () => {
    if (hasRequestedReadRef.current) return;

    hasRequestedReadRef.current = true;
    markAsRead();
  };

  useEffect(() => {
    const content = contentRef.current;

    if (!content || content.scrollHeight > content.clientHeight) return;

    setReadProgress(100);
    if (hasRequestedReadRef.current) return;

    hasRequestedReadRef.current = true;
    markAsRead();
  }, [markAsRead]);

  const updateReadProgress = (event: UIEvent<HTMLElement>) => {
    const { scrollTop, scrollHeight, clientHeight } = event.currentTarget;
    const scrollableHeight = scrollHeight - clientHeight;
    const nextProgress =
      scrollableHeight <= 0 ? 100 : Math.round((scrollTop / scrollableHeight) * 100);

    setReadProgress((current) => Math.max(current, nextProgress));

    if (nextProgress >= 100) {
      requestMarkAsRead();
    }
  };

  const isRead = readStatus.isRead || updateReadMutation.isSuccess;
  const readAt = updateReadMutation.data?.readAt ?? readStatus.readAt;

  return (
    <>
      <Main ref={contentRef} css={contentStyle} onScroll={updateReadProgress}>
        <NoticeArticle notice={notice} hasTopMargin={false} />
      </Main>

      <MemberNoticeReadState
        progress={readProgress}
        isRead={isRead}
        readAt={readAt ? formatRelativeTime(readAt) : undefined}
        showCompletionToast={updateReadMutation.isSuccess && !readStatus.isRead}
      />
    </>
  );
}
