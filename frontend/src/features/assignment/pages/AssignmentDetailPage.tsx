import MemberAssignmentDetailPage from './MemberAssignmentDetailPage';
import LeaderAssignmentDetailpage from './LeaderAssginmentDetailPage';
import { useSuspenseQuery } from '@tanstack/react-query';
import useStudyId from '../../study/hooks/useStudyId';
import studyQueries from '../../study/queries';

export default function AssignmentDetailPage() {
  const { studyId } = useStudyId();

  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? <LeaderAssignmentDetailpage /> : <MemberAssignmentDetailPage />;
}
