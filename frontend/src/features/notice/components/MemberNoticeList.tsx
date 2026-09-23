import Badge from '../../../shared/ui/Badge';
import type { MemberNoticeSummary, NoticeReadState } from '../types';
import NoticeList from './NoticeList';

interface MemberNoticeListProps {
  notices: MemberNoticeSummary[];
  studyId: number;
}

const readStatusBadge = {
  READ: { variant: 'brandSolid', label: '읽음' },
  UNREAD: { variant: 'brandOutline', label: '읽지 않음' },
  NOT_ASSIGNED: { variant: 'neutralSolid', label: '확인 대상 아님' },
} satisfies Record<NoticeReadState, { variant: React.ComponentProps<typeof Badge>['variant']; label: string }>;

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
