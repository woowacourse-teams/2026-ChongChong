import clock from '../../../shared/assets/clock-black.svg';
import Badge from '../../../shared/ui/Badge';
import { Notice } from '../types';
import NoticeList from './NoticeList';

interface LeaderNoticeListProps {
  notices: Notice[];
  studyId: string;
}

export default function LeaderNoticeList({ notices, studyId }: LeaderNoticeListProps) {
  return (
    <NoticeList notices={notices} studyId={studyId} role="leader">
      {(notice) => (
        <>
          <Badge variant="brandOutline" size="small">
            {notice.readCount}/{notice.totalCount} 읽음
          </Badge>
          {notice.reminderText && (
            <Badge variant="neutralSolid" size="small">
              <img src={clock} alt="리마인드 시각" width={12} height={12} />
              {notice.reminderText}
            </Badge>
          )}
        </>
      )}
    </NoticeList>
  );
}
