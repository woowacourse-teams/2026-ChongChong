import { CSSProperties } from 'react';
import AssignmentList from './AssignmentList';
import { tokens } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import { useNavigate } from 'react-router';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { useSuspenseQuery } from '@tanstack/react-query';
import assignmentQueries from '../queries';
import Badge from '../../../shared/ui/Badge';
import clock from '../../../shared/assets/clock.svg';
import { formatReminder } from '../../../shared/utils/formatDate';

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
                    모두 제출
                  </Badge>
                ) : (
                  <Badge variant="brandOutline" size="small">
                    {assignment.completeCount}/{assignment.memberCount} 제출
                  </Badge>
                )}

                {assignment.remindAt && (
                  <Badge variant="neutralSolid" size="small">
                    <img src={clock} alt="리마인드 시각" width={12} height={12} />
                    {formatReminder(assignment.remindAt) + ' 리마인드'}
                  </Badge>
                )}
              </>
            )}
          </AssignmentList>
          <div css={buttonAreaStyle}>
            <Button
              variant="brandSolid"
              size="large"
              onClick={() => navigate(`/studies/${studyId}/assignments/create`)}
            >
              공지 작성하기
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
