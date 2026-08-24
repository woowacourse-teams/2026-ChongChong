import LeaderAssignmentListPage from './LeaderAssignmentListPage';
import MemberAssignmentListPage from './MemberAssignmentListPage';

export default function AssignmentListPage() {
  const role = 'leader';

  return role === 'leader' ? <LeaderAssignmentListPage /> : <MemberAssignmentListPage />;
}
