import { CSSProperties } from 'react';
import AssignmentList from './AssignmentList';
import { tokens } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import { useNavigate } from 'react-router';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import assignmentQueries from '../queries';
import Badge from '../../../shared/ui/Badge';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';
// import clock from '../../../shared/assets/clock.svg';
// import { formatReminder } from '../../../shared/utils/formatDate';

interface Props {
  studyId: number;
}

const buttonAreaStyle = {
  display: 'flex',
  width: '100%',
  justifyContent: 'center',
  marginTop: tokens.spacing[3],
} satisfies CSSProperties;

export default function LeaderAssignmentListSection({ studyId }: Props) {
  const navigate = useNavigate();

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
        <EmptyContent message="아직 공지가 없어요" />
      ) : (
        <>
          <AssignmentList assignments={assignments} studyId={studyId}>
            {(assignment) => (
              <>
                {assignment.isComplete ? (
                  <Badge variant="brandSolid" size="small">
                    모두 제출
                  </Badge>
                ) : (
                  <Badge variant="brandOutline" size="small">
                    {assignment.completeCount ?? 0}/{assignment.memberCount ?? 0} 제출
                  </Badge>
                )}
                {/* 당장 필요하지 않은 리마인드 정보 주석 처리 */}
                {/* {assignment.remindAt && (
                  <Badge variant="neutralSolid" size="small">
                    <img src={clock} alt="리마인드 시각" width={12} height={12} />
                    {formatReminder(assignment.remindAt) + ' 리마인드'}
                  </Badge>
                )} */}
              </>
            )}
          </AssignmentList>
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
          {isFetchingNextPage && <p role="status">과제를 더 불러오는 중...</p>}
          <div css={buttonAreaStyle}>
            <Button
              variant="brandSolid"
              size="large"
              onClick={() => navigate(`/studies/${studyId}/assignments/create`)}
            >
              과제 작성하기
            </Button>
          </div>
        </>
      )}
    </section>
  );
}
