import LeaderAssignmentListPage from './LeaderAssignmentListPage';
import MemberAssignmentListPage from './MemberAssignmentListPage';
import { useSuspenseQuery } from '@tanstack/react-query';
import useStudyId from '../../studies/hooks/useStudyId';
import studyQueries from '../../studies/queries';

export default function AssignmentListPage() {
  const { studyId } = useStudyId();

  const {
    data: { role, studyName, memberName },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? (
    <LeaderAssignmentListPage studyName={studyName} memberName={memberName} />
  ) : (
    <MemberAssignmentListPage studyName={studyName} memberName={memberName} />
  );
}
