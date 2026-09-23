import { CSSProperties } from 'react';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import Badge from '../../../shared/ui/Badge';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';
import noticeQueries from '../queries';
import NoticeList from './NoticeList';
import type { MemberNoticeSummary, NoticeReadState } from '../types';

interface Props {
  studyId: number;
}

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

const readStatusBadge = {
  READ: { variant: 'brandSolid', label: '읽음' },
  UNREAD: { variant: 'brandOutline', label: '읽지 않음' },
  NOT_ASSIGNED: { variant: 'neutralSolid', label: '확인 대상 아님' },
} satisfies Record<NoticeReadState, { variant: React.ComponentProps<typeof Badge>['variant']; label: string }>;

export default function MemberNoticeListContent({ studyId }: Props) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    noticeQueries.list(studyId),
  );
  const notices = data.pages
    .flatMap((page) => page.notices)
    .filter((notice): notice is MemberNoticeSummary => 'readStatus' in notice);
  const loadMoreRef = useInfiniteScroll({
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  });

  return (
    <section css={sectionStyle}>
      {notices.length === 0 ? (
        <EmptyContent message="아직 공지가 없어요" />
      ) : (
        <>
          <NoticeList notices={notices} studyId={studyId}>
            {(notice) => {
              const badge = readStatusBadge[notice.readStatus];

              return (
                <Badge variant={badge.variant} size="small">
                  {badge.label}
                </Badge>
              );
            }}
          </NoticeList>
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
          {isFetchingNextPage && <p role="status">공지를 더 불러오는 중...</p>}
        </>
      )}
    </section>
  );
}
