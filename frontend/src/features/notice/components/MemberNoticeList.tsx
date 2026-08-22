import Badge from '../../../shared/ui/Badge';
import { Notice } from '../types';
import NoticeList from './NoticeList';

interface MemberNoticeListProps {
  notices: Notice[];
  studyId: string;
}

export default function MemberNoticeList({ notices, studyId }: MemberNoticeListProps) {
  return (
    <NoticeList notices={notices} studyId={studyId} role="leader">
      {(notice) => (
        <Badge variant={notice.isRead ? 'brandOutline' : 'brandSolid'} size="small">
          {notice.isRead ? '읽음' : '읽지 않음'}
        </Badge>
      )}
    </NoticeList>
  );
}
