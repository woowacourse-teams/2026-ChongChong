import LeaderNoticeDetailPage from './LeaderNoticeDetailPage';
import MemberNoticeDetailPage from './MemberNoticeDetailPage';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import studyQueries from '../../study/queries';
import { useSuspenseQuery } from '@tanstack/react-query';

export default function NoticeDetailPage() {
  const { studyId } = useIntegerParams(['studyId']);

  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return role === 'LEADER' ? <LeaderNoticeDetailPage /> : <MemberNoticeDetailPage />;
}
