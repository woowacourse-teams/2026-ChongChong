import { useSuspenseQueries, useSuspenseQuery } from '@tanstack/react-query';
import { Suspense } from 'react';
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

  return (
    <>
      <SubmitStatusSection status={submitStatusResponse} />
      <AssignmentArticle assignment={assignment} />
      <SubmissionList submissions={submissions.submissions} />

      {assignment.submissionTarget === 'MEMBERS_AND_LEADER' && (
        <Suspense fallback={null}>
          <LeaderMyAssignmentSubmission studyId={studyId} assignmentId={assignmentId} />
        </Suspense>
      )}
    </>
  );
}

function LeaderMyAssignmentSubmission({
  studyId,
  assignmentId,
}: {
  studyId: number;
  assignmentId: number;
}) {
  const { data: submission } = useSuspenseQuery(
    assignmentQueries.mySubmission(studyId, assignmentId),
  );

  return (
    <MyAssignmentSubmission studyId={studyId} assignmentId={assignmentId} submission={submission} />
  );
}
