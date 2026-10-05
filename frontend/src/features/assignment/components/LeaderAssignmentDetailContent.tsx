import { useQuery, useSuspenseQueries } from '@tanstack/react-query';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import DetailTabs from '../../../shared/widgets/DetailTabs';
import IncompleteMemberList from '../../../shared/widgets/IncompleteMemberList';
import ContentDetailHeader from '../../../shared/widgets/ContentDetailHeader';
import { formatDateToString } from '../../../shared/utils/formatDate';
import SubmitStatus from './SubmitStatus';
import AssignmentArticle from './AssignmentArticle';
import CompletedSubmissionList from './CompletedSubmissionList';
import assignmentQueries from '../queries';
import MyAssignmentSubmission from './MyAssignmentSubmission';
import MySubmissionStatus from './MySubmissionStatus';

interface Props {
  studyId: number;
  userName: string;
}

export default function LeaderAssignmentDetailContent({ studyId, userName }: Props) {
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
  const myCompletedSubmission =
    mySubmission?.submissionStatus === 'SUBMITTED'
      ? submissions.submissions.find(({ id }) => id === mySubmission.submissionId)
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
            <SubmitStatus status={submitStatusResponse} />
            {mySubmission ? (
              <MySubmissionStatus
                studyId={studyId}
                assignmentId={assignmentId}
                submission={mySubmission}
                member={member}
              />
            ) : null}
            <CompletedSubmissionList submissions={submissions.submissions} />
            <IncompleteMemberList
              title={`미제출 ${submitStatusResponse.incompleteCount}명`}
              members={submitStatusResponse.incompleteMembers}
            />
          </>
        }
        detail={
          <>
            <AssignmentArticle assignment={assignment} />
            {assignment.submissionTarget === 'MEMBERS_AND_LEADER' && mySubmission ? (
              <MyAssignmentSubmission
                studyId={studyId}
                assignmentId={assignmentId}
                submission={mySubmission}
              />
            ) : null}
          </>
        }
      />
    </>
  );
}
