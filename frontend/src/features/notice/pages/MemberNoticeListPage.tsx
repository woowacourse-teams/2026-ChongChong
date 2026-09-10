import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import Page from '../../../shared/ui/Page';
import BottomTab from '../../../shared/widgets/BottomTab';
import { Suspense } from 'react';
import Loading from '../../../shared/ui/Loading';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import MemberNoticeListSection from '../components/MemerNoticeListSection';

interface Props {
  studyName: string;
  userName: string;
}

export default function MemberNoticeListPage({ studyName, userName }: Props) {
  const { studyId } = useIntegerParams(['studyId']);

  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={
          <>
            <TopHeader.Title>{studyName}</TopHeader.Title>
            <TopHeader.Subtitle>{userName} · 스터디원</TopHeader.Subtitle>
          </>
        }
      />
      <Main>
        <Suspense fallback={<Loading />}>
          <MemberNoticeListSection studyId={studyId} />
        </Suspense>
      </Main>
      <BottomTab />
    </Page>
  );
}
