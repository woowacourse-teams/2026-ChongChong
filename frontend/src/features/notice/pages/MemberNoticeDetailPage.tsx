import { Suspense } from 'react';
import { useParams } from 'react-router';
import BottomTab from '../../../shared/widgets/BottomTab';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import Loading from '../../../shared/ui/Loading';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import MemberNoticeDetailContent from '../components/MemberNoticeDetailContent';

export default function MemberNoticeDetailPage() {
  const { studyId, noticeId } = useParams();

  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>공지</TopHeader.Title>} />
      <Suspense fallback={<Loading />}>
        <MemberNoticeDetailContent studyId={Number(studyId)} noticeId={Number(noticeId)} />
      </Suspense>
      <BottomTab />
    </Page>
  );
}
