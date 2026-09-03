import { Suspense } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import MemberListContent from '../components/MemberListContent';
import useStudyId from '../../study/hooks/useStudyId';
import BottomTab from '../../../shared/widgets/BottomTab';
import Loading from '../../../shared/ui/Loading';
import studyQueries from '../../study/queries';

export default function MemberListPage() {
  const { studyId } = useStudyId();
  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return (
    <Page>
      <Suspense fallback={<Loading />}>
        <TopHeader left={<PrevButton />} middle={<TopHeader.Title>멤버</TopHeader.Title>} />
        <Main>
          {role === 'LEADER' ? <MemberListContent.Leader /> : <MemberListContent.Member />}
        </Main>
      </Suspense>
      <BottomTab />
    </Page>
  );
}
