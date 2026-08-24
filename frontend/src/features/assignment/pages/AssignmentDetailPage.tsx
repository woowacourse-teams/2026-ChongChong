import MemberAssginmentDetailPage from './MemberAssignmentDetailPage';
import LeaderAssignmentDetailpage from './LeaderAssginmentDetailPage';
import { useParams } from 'react-router';
import { useSuspenseQuery } from '@tanstack/react-query';
import studyQueries from '../../studies/queries';

export default function AssignmentDetailPage() {
  const { studyId } = useParams();

  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId!));

  return role === 'LEADER' ? <LeaderAssignmentDetailpage /> : <MemberAssginmentDetailPage />;
}
