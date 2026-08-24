import LeaderAssignmentListPage from './LeaderAssignmentListPage';
import MemberAssignmentListPage from './MemberAssignmentListPage';
import { useParams } from 'react-router';
import { useSuspenseQuery } from '@tanstack/react-query';
import studyQueries from '../../studies/queries';

export default function AssignmentListPage() {
  const { studyId } = useParams();

  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId!));

  return role === 'LEADER' ? <LeaderAssignmentListPage /> : <MemberAssignmentListPage />;
}
