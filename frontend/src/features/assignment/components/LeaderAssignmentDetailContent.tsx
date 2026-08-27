import { useSuspenseQueries } from '@tanstack/react-query';
import DetailActions from '../../../shared/ui/components/DetailActions';
import assignmentQueries from '../queries';
import AssignmentArticle from './AssignmentArticle';
import SubmissionList from './SubmissionList';
import SubmitStatusSection from './SubmitStatusSection';

interface Props {
  studyId: number;
  assignmentId: number;
  onEdit: () => void;
  onDelete: () => void;
}

export default function LeaderAssignmentDetailContent({
  studyId,
  assignmentId,
  onEdit,
  onDelete,
}: Props) {
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

      <DetailActions onEdit={onEdit} onDelete={onDelete} />
    </>
  );
}
