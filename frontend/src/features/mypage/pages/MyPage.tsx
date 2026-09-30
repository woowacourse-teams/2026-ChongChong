import { Suspense } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import TopHeader from '../../../shared/ui/TopHeader';
import Page from '../../../shared/ui/Page';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import AccountMenuSection from '../components/AccountMenuSection';
import ProfileSection from '../components/ProfileSection';
import Main from '../../../shared/ui/Main';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';

export default function MyPage() {
  return (
    <Page>
      <TopHeader left={<PrevButton />} middle={<TopHeader.Title>마이페이지</TopHeader.Title>} />

      <Main>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <Suspense fallback={<Loading />}>
            <ProfileSection />
            <AccountMenuSection />
          </Suspense>
        </ErrorBoundary>
      </Main>
    </Page>
  );
}
