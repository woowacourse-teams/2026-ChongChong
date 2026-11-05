import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { Paragraph } from '../../../shared/ui/Typography';
import { CSSProperties } from 'react';
import { tokens } from '../../../styles/global';
import Floating from '../../../shared/ui/Floating';
import FloatingMenu from '../../../shared/widgets/floating/FloatingMenu';
import plus from '../../../shared/assets/icons/plus-white.svg';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import Badge from '../../../shared/ui/Badge';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';
import noticeQueries from '../queries';
import NoticeList from './NoticeList';
import type { LeaderNoticeSummary } from '../types';

interface Props {
  studyId: number;
}

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  paddingBottom: tokens.spacing[20],
} satisfies CSSProperties;

export default function LeaderNoticeListContent({ studyId }: Props) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    noticeQueries.list(studyId),
  );
  const notices = data.pages
    .flatMap((page) => page.notices)
    .filter((notice): notice is LeaderNoticeSummary => 'isComplete' in notice);
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
                    모두 읽음
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
          {isFetchingNextPage && (
            <Paragraph variant="inherited" role="status">
              공지를 더 불러오는 중...
            </Paragraph>
          )}
        </>
      )}
      <FloatingMenu
        aria-label="공지 메뉴"
        bottom={`calc(${tokens.layout.tabBarHeight} + ${tokens.layout.tabBarBottomInset} + ${tokens.spacing[4]})`}
        trigger={({ bottom, ...props }) => (
          <Floating
            {...props}
            css={{ bottom }}
            aria-label="공지 메뉴 열기"
            icon={<img src={plus} alt="" width={30} height={30} />}
          />
        )}
      >
        <FloatingMenu.LinkItem
          {...URL_POLICY.toCreateNotice(studyId)}
          icon={<img src={plus} alt="" width={22} height={22} />}
          label="공지 작성하기"
          description="스터디원에게 전할 소식을 작성해요"
        />
      </FloatingMenu>
    </section>
  );
}
