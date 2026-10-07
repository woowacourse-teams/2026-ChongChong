import { useQuery, useSuspenseQueries } from '@tanstack/react-query';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import ContentDetailHeader from '../../../shared/widgets/ContentDetailHeader';
import DetailTabs from '../../../shared/widgets/DetailTabs';
import { formatDateToString } from '../../../shared/utils/formatDate';
import assignmentQueries from '../queries';
import AssignmentArticle from './AssignmentArticle';
import CompletedSubmissionList from './CompletedSubmissionList';
import MyAssignmentSubmission from './MyAssignmentSubmission';
import MySubmissionStatus from './MySubmissionStatus';
import { isCompletedAssignment } from '../submissionStatus';

interface Props {
  studyId: number;
  userName: string;
}

export default function MemberAssignmentDetailContent({ studyId, userName }: Props) {
  const { assignmentId } = useIntegerParams(['assignmentId']);
  const [{ data: assignment }, { data: submission }] = useSuspenseQueries({
    queries: [
      assignmentQueries.detail(studyId, assignmentId),
      assignmentQueries.mySubmission(studyId, assignmentId),
    ],
  });
  const canViewSubmissions = assignment.submissionVisibility === 'ALL_STUDY_MEMBERS';
  const { data: submissions } = useQuery({
    ...assignmentQueries.submissions(studyId, assignmentId),
    enabled: canViewSubmissions,
  });
  const myCompletedSubmission = isCompletedAssignment(submission)
    ? submissions?.submissions.find(({ id }) => id === submission.submissionId)
    : undefined;
  const member = {
    name: myCompletedSubmission?.name ?? userName,
    profileImage: myCompletedSubmission?.profileImage ?? null,
  };

  return (
    <>
      <ContentDetailHeader
        title={assignment.title}
        dateTime={assignment.closeAt}
        meta={`${formatDateToString(assignment.closeAt)} 마감`}
      />
      <DetailTabs
        summary={
          <>
            <MySubmissionStatus submission={submission} member={member} />
            {canViewSubmissions && submissions ? (
              <CompletedSubmissionList submissions={submissions.submissions} />
            ) : null}
          </>
        }
        detail={
          <>
            <AssignmentArticle assignment={assignment} />
            <MyAssignmentSubmission
              studyId={studyId}
              assignmentId={assignmentId}
              submission={submission}
            />
          </>
        }
      />
    </>
  );
}
