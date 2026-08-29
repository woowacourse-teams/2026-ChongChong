import LeaderNoticeDetailPage from './LeaderNoticeDetailPage';
import MemberNoticeDetailPage from './MemberNoticeDetailPage';
import useStudyId from '../../study/hooks/useStudyId';
import studyQueries from '../../study/queries';
import { useSuspenseQuery } from '@tanstack/react-query';

export default function NoticeDetailPage() {
  const { studyId } = useStudyId();

  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? <LeaderNoticeDetailPage /> : <MemberNoticeDetailPage />;
}
