import Badge from '../../../shared/ui/Badge';
import type { MemberNoticeSummary } from '../types';
import NoticeList from './NoticeList';
import { readStatusBadge } from '../constants';

interface MemberNoticeListProps {
  notices: MemberNoticeSummary[];
  studyId: number;
}

export default function MemberNoticeList({ notices, studyId }: MemberNoticeListProps) {
  return (
    <NoticeList notices={notices} studyId={studyId}>
      {(notice) => {
        const badge = readStatusBadge[notice.readStatus];

        return (
          <Badge variant={badge.variant} size="small">
            {badge.label}
          </Badge>
        );
      }}
    </NoticeList>
  );
}
