import { useSuspenseQueries } from '@tanstack/react-query';
import DetailActions from '../../../shared/widgets/DetailActions';
import noticeQueries from '../queries';
import NoticeArticle from './NoticeArticle';
import NoticeReadStatus from './NoticeReadStatus';

interface Props {
  studyId: number;
  noticeId: number;
  onEdit: () => void;
  onDelete: () => void;
}

export default function LeaderNoticeDetailContent({ studyId, noticeId, onEdit, onDelete }: Props) {
  const [{ data: notice }, { data: readStatus }] = useSuspenseQueries({
    queries: [noticeQueries.detail(studyId, noticeId), noticeQueries.readStatus(studyId, noticeId)],
  });

  return (
    <>
      <NoticeReadStatus status={readStatus} />
      <NoticeArticle notice={notice} />
      <div css={{ marginTop: 'auto' }}>
        <DetailActions onEdit={onEdit} onDelete={onDelete} />
      </div>
    </>
  );
}
