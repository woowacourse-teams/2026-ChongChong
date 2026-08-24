import { useSearchParams } from 'react-router';
import LeaderNoticeDetailPage from './LeaderNoticeDetailPage';
import MemberNoticeDetailPage from './MemberNoticeDetailPage';

export default function NoticeDetailPage() {
  const [searchParams] = useSearchParams();

  return searchParams.get('role') === 'leader' ? (
    <LeaderNoticeDetailPage />
  ) : (
    <MemberNoticeDetailPage />
  );
}
