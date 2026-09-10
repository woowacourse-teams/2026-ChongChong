import { useParams } from 'react-router';

export default function useAssignmentId() {
  const { assignmentId } = useParams();
  const parsed = Number(assignmentId);

  if (!assignmentId || !Number.isInteger(parsed)) {
    throw new Error(`'${assignmentId}'는 유효하지 않은 assignmentId 입니다.`);
  }

  return { assignmentId: parsed };
}
