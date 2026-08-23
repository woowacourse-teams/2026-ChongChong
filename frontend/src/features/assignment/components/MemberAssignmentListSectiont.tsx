import AssignmentList from './AssignmentList';
import EmptyState from '../../../shared/ui/EmptyState';
import { useSuspenseQuery } from '@tanstack/react-query';
import assignmentQueries from '../queries';
import Badge from '../../../shared/ui/Badge';

interface Props {
  studyId: number;
}

export default function MemberAssignmentListSection({ studyId }: Props) {
  const { data: assignments } = useSuspenseQuery({
    ...assignmentQueries.list(studyId),
    select: (data) => data.assignments,
  });

  return (
    <div>
      {assignments.length === 0 ? (
        <EmptyState message="아직 공지가 없어요" />
      ) : (
        <>
          <AssignmentList assignments={assignments} studyId={studyId}>
            {(assignment) => (
              <>
                {assignment.isComplete ? (
                  <Badge variant="BrandSolid" size="Small">
                    제출 완료
                  </Badge>
                ) : (
                  <Badge variant="BrandOutline" size="Small">
                    미제출
                  </Badge>
                )}
              </>
            )}
          </AssignmentList>
        </>
      )}
    </div>
  );
}
