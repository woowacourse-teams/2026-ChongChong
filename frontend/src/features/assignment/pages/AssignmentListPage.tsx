import LeaderAssignmentListPage from './LeaderAssignmentListPage';
import MemberAssignmentListPage from './MemberAssignmentListPage';
import { useSuspenseQuery } from '@tanstack/react-query';
import useStudyId from '../../study/hooks/useStudyId';
import studyQueries from '../../study/queries';

export default function AssignmentListPage() {
  const { studyId } = useStudyId();

  const {
    data: { role, studyName, userName },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? (
    <LeaderAssignmentListPage studyName={studyName} userName={userName} />
  ) : (
    <MemberAssignmentListPage studyName={studyName} userName={userName} />
  );
}
