import { CSSProperties } from 'react';
import { tokens } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import { useNavigate } from 'react-router';
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

const buttonAreaStyle = {
  display: 'flex',
  width: '100%',
  justifyContent: 'center',
  marginTop: tokens.spacing[3],
} satisfies CSSProperties;

export default function LeaderNoticeListSection({ studyId }: Props) {
  const navigate = useNavigate();

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
                    모두 제출
                  </Badge>
                ) : (
                  <Badge variant="brandOutline" size="small">
                    {notice.readRecipientCount}/{notice.recipientCount} 확인
                  </Badge>
                )}
              </>
            )}
          </NoticeList>
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
          {isFetchingNextPage && <p role="status">공지를 더 불러오는 중...</p>}
        </>
      )}
      <div css={buttonAreaStyle}>
        <Button
          variant="brandSolid"
          size="large"
          onClick={() => navigate(`/studies/${studyId}/assignments/create`)}
        >
          공지 작성하기
        </Button>
      </div>
    </section>
  );
}
