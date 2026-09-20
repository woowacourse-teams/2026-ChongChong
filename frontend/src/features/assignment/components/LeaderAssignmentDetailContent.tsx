import { useQuery, useSuspenseQueries } from '@tanstack/react-query';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import SubmitStatusSection from './SubmitStatusSection';
import AssignmentArticle from './AssignmentArticle';
import SubmissionList from './SubmissionList';
import assignmentQueries from '../queries';
import MyAssignmentSubmission from './MyAssignmentSubmission';

interface Props {
  studyId: number;
}

export default function LeaderAssignmentDetailContent({ studyId }: Props) {
  const { assignmentId } = useIntegerParams(['assignmentId']);

  const [{ data: assignment }, { data: submitStatusResponse }, { data: submissions }] =
    useSuspenseQueries({
      queries: [
        assignmentQueries.detail(studyId, assignmentId),
        assignmentQueries.submitStatus(studyId, assignmentId),
        assignmentQueries.submissions(studyId, assignmentId),
      ],
    });
  const { data: mySubmission } = useQuery({
    ...assignmentQueries.mySubmission(studyId, assignmentId),
    enabled: assignment.submissionTarget === 'MEMBERS_AND_LEADER',
    throwOnError: true,
  });

  return (
    <>
      <SubmitStatusSection status={submitStatusResponse} />
      <AssignmentArticle assignment={assignment} />
      <SubmissionList submissions={submissions.submissions} />

      {assignment.submissionTarget === 'MEMBERS_AND_LEADER' && mySubmission && (
        <MyAssignmentSubmission
          studyId={studyId}
          assignmentId={assignmentId}
          submission={mySubmission}
        />
      )}
    </>
  );
}
