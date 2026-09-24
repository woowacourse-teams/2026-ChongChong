import { useSuspenseQueries } from '@tanstack/react-query';
import noticeQueries from '../queries';
import NoticeArticle from './NoticeArticle';
import NoticeReadStatus from './NoticeReadStatus';

interface Props {
  studyId: number;
  noticeId: number;
}

export default function LeaderNoticeDetailContent({ studyId, noticeId }: Props) {
  const [{ data: notice }, { data: readStatus }] = useSuspenseQueries({
    queries: [noticeQueries.detail(studyId, noticeId), noticeQueries.readStatus(studyId, noticeId)],
  });

  return (
    <>
      <NoticeReadStatus status={readStatus} />
      <NoticeArticle notice={notice} />
    </>
  );
}
