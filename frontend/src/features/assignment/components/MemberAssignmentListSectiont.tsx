import AssignmentList from './AssignmentList';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import assignmentQueries from '../queries';
import Badge from '../../../shared/ui/Badge';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';

interface Props {
  studyId: number;
}

export default function MemberAssignmentListSection({ studyId }: Props) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    assignmentQueries.list(studyId),
  );
  const assignments = data.pages.flatMap((page) => page.assignments);
  const loadMoreRef = useInfiniteScroll({
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  });

  return (
    <section>
      {assignments.length === 0 ? (
        <EmptyContent message="아직 과제가 없어요" />
      ) : (
        <>
          <AssignmentList assignments={assignments} studyId={studyId}>
            {(assignment) => (
              <>
                {assignment.isComplete ? (
                  <Badge variant="brandSolid" size="small">
                    제출 완료
                  </Badge>
                ) : (
                  <Badge variant="brandOutline" size="small">
                    미제출
                  </Badge>
                )}
              </>
            )}
          </AssignmentList>
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
          {isFetchingNextPage && <p role="status">과제를 더 불러오는 중...</p>}
        </>
      )}
    </section>
  );
}
