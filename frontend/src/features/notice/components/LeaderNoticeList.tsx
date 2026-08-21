import clock from '../../../shared/assets/clock-black.svg';
import Badge from '../../../shared/ui/Badge';
import NoticeList, { type NoticeListItem } from './NoticeList';

interface LeaderNoticeListProps {
  notices: NoticeListItem[];
  studyId: string;
}

export default function LeaderNoticeList({ notices, studyId }: LeaderNoticeListProps) {
  return (
    <NoticeList notices={notices} studyId={studyId} detailSearch="?role=leader">
      {(notice) => (
        <>
          <Badge variant="BrandOutline" size="Small">
            {notice.readCount}/{notice.totalCount} 읽음
          </Badge>
          {notice.reminderText && (
            <Badge variant="NeutralSolid" size="Small">
              <img src={clock} alt="리마인드 시각" width={12} height={12} />
              {notice.reminderText}
            </Badge>
          )}
        </>
      )}
    </NoticeList>
  );
}
