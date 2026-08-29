import { useParams } from 'react-router';

export default function useStudyId() {
  const { studyId } = useParams();
  const parsed = Number(studyId);

  if (!studyId || !Number.isInteger(parsed)) {
    throw new Error(`'${studyId}'는 유효하지 않은 studyId 입니다.`);
  }

  return { studyId: parsed };
}
