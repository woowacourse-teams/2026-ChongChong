import { CSSProperties } from 'react';
import EmptyContent from '../../../shared/ui/EmptyContent';
import Spinner from '../../../shared/ui/Spinner';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import Badge from '../../../shared/ui/Badge';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';
import noticeQueries from '../queries';
import NoticeList from './NoticeList';
import type { MemberNoticeSummary } from '../types';
import { readStatusBadge } from '../constants';

interface Props {
  studyId: number;
}

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

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
          {isFetchingNextPage && <Spinner size={24} label="공지를 더 불러오는 중" />}
        </>
      )}
    </section>
  );
}
