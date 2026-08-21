import { useSearchParams } from 'react-router';
import LeaderNoticeListPage from './LeaderNoticeListPage';
import MemberNoticeListPage from './MemberNoticeListPage';

export default function NoticeListPage() {
  const [searchParams] = useSearchParams();

  return searchParams.get('role') === 'leader' ? (
    <LeaderNoticeListPage />
  ) : (
    <MemberNoticeListPage />
  );
}
