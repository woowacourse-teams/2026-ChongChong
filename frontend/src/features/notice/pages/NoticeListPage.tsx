import LeaderNoticeListPage from './LeaderNoticeListPage';
import MemberNoticeListPage from './MemberNoticeListPage';
import studyQueries from '../../studies/queries';
import useStudyId from '../../studies/hooks/useStudyId';
import { useSuspenseQuery } from '@tanstack/react-query';

export default function NoticeListPage() {
  const { studyId } = useStudyId();
  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? <LeaderNoticeListPage /> : <MemberNoticeListPage />;
}
