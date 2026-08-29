import Badge from '../../../shared/ui/Badge';
import { Notice } from '../types';
import NoticeList from './NoticeList';

interface MemberNoticeListProps {
  notices: Notice[];
  studyId: number;
}

export default function MemberNoticeList({ notices, studyId }: MemberNoticeListProps) {
  return (
    <NoticeList notices={notices} studyId={studyId}>
      {(notice) => (
        <Badge variant={notice.isComplete ? 'brandSolid' : 'brandOutline'} size="small">
          {notice.isComplete ? '읽음' : '읽지 않음'}
        </Badge>
      )}
    </NoticeList>
  );
}
