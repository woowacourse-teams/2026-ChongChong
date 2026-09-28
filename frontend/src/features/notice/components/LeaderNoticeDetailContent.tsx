import { useSuspenseQueries } from '@tanstack/react-query';
import DetailTabs from '../../../shared/widgets/DetailTabs';
import MemberStatusList from '../../../shared/widgets/MemberStatusList';
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
      <DetailTabs
        summary={
          <>
            <NoticeReadStatus status={readStatus} />
            <ReadMemberList members={readStatus.readMembers} />
            <MemberStatusList
              title={`미확인 ${readStatus.unreadCount}명`}
              members={readStatus.unreadMembers}
            />
          </>
        }
        detail={<NoticeArticle notice={notice} />}
      />
    </>
  );
}
