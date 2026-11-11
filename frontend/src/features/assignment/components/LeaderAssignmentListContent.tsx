import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import Spinner from '../../../shared/ui/Spinner';
import { CSSProperties } from 'react';
import AssignmentList from './AssignmentList';
import { tokens } from '../../../styles/global';
import Floating from '../../../shared/ui/Floating';
import FloatingMenu from '../../../shared/widgets/floating/FloatingMenu';
import plus from '../../../shared/assets/icons/plus-white.svg';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import assignmentQueries from '../queries';
import Badge from '../../../shared/ui/Badge';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';
import type { LeaderAssignmentSummary } from '../types';
// import clock from '../../../shared/assets/clock.svg';
// import { formatReminder } from '../../../shared/utils/formatDate';

interface Props {
  studyId: number;
}

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  paddingBottom: tokens.spacing[20],
} satisfies CSSProperties;

export default function LeaderAssignmentListContent({ studyId }: Props) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useSuspenseInfiniteQuery(
    assignmentQueries.list(studyId),
  );
  const assignments = data.pages
    .flatMap((page) => page.assignments)
    .filter((assignment): assignment is LeaderAssignmentSummary => 'isComplete' in assignment);
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
            {(assignment) => (
              <>
                {assignment.isComplete ? (
                  <Badge variant="brandSolid" size="small">
                    모두 제출
                  </Badge>
                ) : (
                  <Badge variant="brandOutline" size="small">
                    {assignment.completeCount}/{assignment.memberCount} 제출
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
          {isFetchingNextPage && <Spinner size={24} label="공지를 더 불러오는 중" />}
        </>
      )}
      <FloatingMenu
        aria-label="과제 메뉴"
        bottom={`calc(${tokens.layout.tabBarHeight} + ${tokens.layout.tabBarBottomInset} + ${tokens.spacing[4]})`}
        trigger={({ bottom, ...props }) => (
          <Floating
            {...props}
            css={{ bottom }}
            aria-label="과제 메뉴 열기"
            icon={<img src={plus} alt="" width={30} height={30} />}
          />
        )}
      >
        <FloatingMenu.LinkItem
          {...URL_POLICY.toCreateAssignment(studyId)}
          icon={<img src={plus} alt="" width={22} height={22} />}
          label="과제 추가하기"
          description="함께 진행할 과제를 추가해요"
        />
      </FloatingMenu>
    </section>
  );
}
