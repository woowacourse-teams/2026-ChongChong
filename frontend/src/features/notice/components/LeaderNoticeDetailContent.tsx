import type { NoticeTab } from '../types';
import { useSuspenseQueries } from '@tanstack/react-query';
import Tabs from '../../../shared/widgets/Tabs';
import IncompleteMemberList from '../../../shared/widgets/IncompleteMemberList';
import ContentDetailHeader from '../../../shared/widgets/ContentDetailHeader';
import { formatDateToString } from '../../../shared/utils/formatDate';
import noticeQueries from '../queries';
import NoticeArticle from './NoticeArticle';
import NoticeReadStatus from './NoticeReadStatus';
import ReadMemberList from './ReadMemberList';

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
      <ContentDetailHeader
        title={notice.title}
        dateTime={notice.createdAt}
        meta={`${formatDateToString(notice.createdAt)} 작성`}
      />
      <Tabs<NoticeTab>
        tabs={[
          { key: 'detail', label: '내용', panel: <NoticeArticle notice={notice} /> },
          {
            key: 'status',
            label: '현황',
            panel: (
              <>
                <NoticeReadStatus status={readStatus} />
                <ReadMemberList members={readStatus.readMembers} />
                <IncompleteMemberList
                  title={`미확인 ${readStatus.unreadCount}명`}
                  members={readStatus.unreadMembers}
                />
              </>
            ),
          },
        ]}
      />
    </>
  );
}
