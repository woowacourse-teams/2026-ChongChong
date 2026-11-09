import { useSuspenseQuery } from '@tanstack/react-query';
import studyQueries from '../queries';
import StudyList from './StudyList';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { CSSProperties, useState } from 'react';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

export default function MyStudies() {
  const { data: studies } = useSuspenseQuery({
    ...studyQueries.list(),
    select: (data) => data.studies,
  });

  // 내 스터디 API는 전체 목록을 반환하므로 화면에서 나누어 표시합니다.
  const [visibleCount, setVisibleCount] = useState(10);
  const loadMoreRef = useInfiniteScroll({
    hasNextPage: visibleCount < studies.length,
    isFetchingNextPage: false,
    fetchNextPage: async () => setVisibleCount((count) => count + 10),
  });

  return (
    <section css={sectionStyle} aria-label="내 스터디">
      {studies.length === 0 ? (
        <EmptyContent message="아직 스터디가 없어요" />
      ) : (
        <>
          <StudyList studies={studies.slice(0, visibleCount)} />
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
        </>
      )}
    </section>
  );
}
