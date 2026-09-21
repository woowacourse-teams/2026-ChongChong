import { useSuspenseQueries } from '@tanstack/react-query';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import assignmentQueries from '../queries';
import AssignmentArticle from './AssignmentArticle';
import MyAssignmentSubmission from './MyAssignmentSubmission';

interface Props {
  studyId: number;
}

export default function MemberAssignmentDetailContent({ studyId }: Props) {
  const { assignmentId } = useIntegerParams(['assignmentId']);
  const [{ data: assignment }, { data: submission }] = useSuspenseQueries({
    queries: [
      assignmentQueries.detail(studyId, assignmentId),
      assignmentQueries.mySubmission(studyId, assignmentId),
    ],
  });

  return (
    <>
      <AssignmentArticle assignment={assignment} />
      <MyAssignmentSubmission
        studyId={studyId}
        assignmentId={assignmentId}
        submission={submission}
      />
    </>
  );
}
