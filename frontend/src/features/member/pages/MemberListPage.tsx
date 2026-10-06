import { Suspense } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import {
  MemberListSectionForLeader,
  MemberListSectionForMember,
} from '../components/MemberListSection';
import {
  InviteStudyLinkSection,
  InviteStudyLinkSectionFallback,
} from '../components/InviteStudyLinkSection';
import BottomTab from '../../../shared/widgets/BottomTab';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';
import studyQueries from '../../study/queries';
import { tokens } from '../../../styles/global';

export default function MemberListPage() {
  return (
    <Page>
      <div css={{ display: 'flex', flexDirection: 'column', height: '100dvh' }}>
        <TopHeader left={<PrevButton />} middle={<TopHeader.Title>멤버</TopHeader.Title>} />
        <Main css={{ minHeight: 0, gap: tokens.spacing[5] }}>
          <ErrorBoundary
            fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
          >
            <Suspense fallback={<Loading />}>
              <Content />
            </Suspense>
          </ErrorBoundary>
        </Main>
        <BottomTab />
      </div>
    </Page>
  );
}

function Content() {
  const { studyId } = useIntegerParams(['studyId']);
  const {
    data: { role },
  } = useSuspenseQuery(studyQueries.info(studyId));

  return (
    <>
      {role === 'LEADER' ? <MemberListSectionForLeader /> : <MemberListSectionForMember />}

      <div css={{ flexShrink: 0 }}>
        <ErrorBoundary
          fallbackRender={({ error }) => (
            <InviteStudyLinkSectionFallback message={getErrorMessage(error)} />
          )}
        >
          <Suspense fallback={<Loading />}>
            <InviteStudyLinkSection studyId={studyId} />
          </Suspense>
        </ErrorBoundary>
      </div>
    </>
  );
}
