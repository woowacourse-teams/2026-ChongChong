import Badge from '../../../shared/ui/Badge';
import NoticeList, { type NoticeListItem } from './NoticeList';

interface MemberNoticeListProps {
  notices: NoticeListItem[];
  studyId: string;
}

export default function MemberNoticeList({ notices, studyId }: MemberNoticeListProps) {
  return (
    <NoticeList notices={notices} studyId={studyId}>
      {(notice) => (
        <Badge variant={notice.isRead ? 'brandOutline' : 'brandSolid'} size="small">
          {notice.isRead ? '읽음' : '읽지 않음'}
        </Badge>
      )}
    </NoticeList>
  );
}
