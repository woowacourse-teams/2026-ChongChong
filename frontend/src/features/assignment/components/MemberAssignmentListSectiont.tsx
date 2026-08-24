import AssignmentList from './AssignmentList';
import EmptyContent from '../../../shared/ui/EmptyContent';
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
        <EmptyContent message="아직 공지가 없어요" />
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
        </>
      )}
    </div>
  );
}
