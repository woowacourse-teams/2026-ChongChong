import LeaderNoticeListPage from './LeaderNoticeListPage';
import MemberNoticeListPage from './MemberNoticeListPage';
import studyQueries from '../../study/queries';
import useStudyId from '../../study/hooks/useStudyId';
import { useSuspenseQuery } from '@tanstack/react-query';

export default function NoticeListPage() {
  const { studyId } = useStudyId();
  const {
    data: { role, studyName, userName },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? (
    <LeaderNoticeListPage studyName={studyName} userName={userName} />
  ) : (
    <MemberNoticeListPage studyName={studyName} userName={userName} />
  );
}
