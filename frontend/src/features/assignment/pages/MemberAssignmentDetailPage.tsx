import { Suspense } from 'react';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import Loading from '../../../shared/ui/Loading';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import MemberAssignmentDetailContent from '../components/MemberAssignmentDetailContent';

export default function MemberAssignmentDetailPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>과제</TopHeader.Title>} />
      <Suspense fallback={<Loading />}>
        <Main>
          <MemberAssignmentDetailContent />
        </Main>
      </Suspense>
      <BottomTab />
    </Page>
  );
}
