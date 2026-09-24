import AssignmentList from './AssignmentList';
import { CSSProperties } from 'react';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import assignmentQueries from '../queries';
import Badge from '../../../shared/ui/Badge';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';
import type { MemberAssignmentSummary } from '../types';
import { submissionStatusBadge } from '../constants';

interface Props {
  studyId: number;
}

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

export default function MemberAssignmentListContent({ studyId }: Props) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    assignmentQueries.list(studyId),
  );
  const assignments = data.pages
    .flatMap((page) => page.assignments)
    .filter((assignment): assignment is MemberAssignmentSummary => !('isComplete' in assignment));
  const loadMoreRef = useInfiniteScroll({
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  });

  return (
    <section css={sectionStyle}>
      {assignments.length === 0 ? (
        <EmptyContent message="아직 과제가 없어요" />
      ) : (
        <>
          <AssignmentList assignments={assignments} studyId={studyId}>
            {(assignment) => {
              const badge = submissionStatusBadge[assignment.submissionStatus];

              return (
                <Badge variant={badge.variant} size="small">
                  {badge.label}
                </Badge>
              );
            }}
          </AssignmentList>
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
          {isFetchingNextPage && <p role="status">과제를 더 불러오는 중...</p>}
        </>
      )}
    </section>
  );
}
