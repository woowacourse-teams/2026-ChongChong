import { CSSProperties } from 'react';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import Badge from '../../../shared/ui/Badge';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';
import noticeQueries from '../queries';
import NoticeList from './NoticeList';

interface Props {
  studyId: number;
}

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

export default function MemberNoticeListSection({ studyId }: Props) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    noticeQueries.list(studyId),
  );
  const notices = data.pages.flatMap((page) => page.notices);
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
            {(notice) => (
              <>
                {notice.isComplete ? (
                  <Badge variant="brandSolid" size="small">
                    읽음
                  </Badge>
                ) : (
                  <Badge variant="brandOutline" size="small">
                    읽지 않음
                  </Badge>
                )}
              </>
            )}
          </NoticeList>
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
          {isFetchingNextPage && <p role="status">공지를 더 불러오는 중...</p>}
        </>
      )}
    </section>
  );
}
