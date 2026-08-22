import { CSSProperties } from 'react';
import AssignmentList from './AssignmentList';
import { tokens } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import { useNavigate } from 'react-router';
import EmptyState from '../../../shared/ui/EmptyState';
import { useSuspenseQuery } from '@tanstack/react-query';
import assignmentQueries from '../queries';

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
        <EmptyState message="아직 공지가 없어요" />
      ) : (
        <>
          <AssignmentList assignments={assignments} studyId={studyId} />
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
