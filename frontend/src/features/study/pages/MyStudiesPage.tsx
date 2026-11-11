import { Link } from 'react-router';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { Suspense, type ReactNode } from 'react';
import Tabs from '../../../shared/widgets/Tabs';
import PublicStudies from '../components/PublicStudies';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import ErrorContent from '../../../shared/ui/ErrorContent';
import TopHeader from '../../../shared/ui/TopHeader';
import logo from '../../../shared/assets/icons/header-icon.svg?width=120&as=webp';
import MyStudies from '../components/MyStudies';
import Main from '../../../shared/ui/Main';
import { tokens } from '../../../styles/global';
import Page from '../../../shared/ui/Page';
import Loading from '../../../shared/ui/Loading';
import NotificationLink from '../../notification/components/NotificationLink';
import MyIcon from '../../../shared/assets/my.webp';
import StudyFloatingActions from '../components/StudyFloatingActions';

export default function MyStudiesPage() {
  return (
    <Page>
      <TopHeader
        left={
          <div>
            <img css={{ width: '40px', height: '40px' }} src={logo} alt="" />
          </div>
        }
        right={
          <div css={{ display: 'flex', gap: tokens.spacing[5], alignItems: 'center' }}>
            <NotificationLink />
            <Link {...URL_POLICY.toMyPage()} aria-label="마이페이지">
              <img css={{ width: '28px', height: '28px' }} src={MyIcon} alt="" />
            </Link>
          </div>
        }
      />

      <Main css={{ paddingBottom: `calc(${tokens.spacing[20]} + ${tokens.layout.safeBottom})` }}>
        <Tabs
          tabs={[
            {
              key: 'my',
              label: '내 스터디',
              panel: (
                <StudyPanel key="my">
                  <MyStudies />
                </StudyPanel>
              ),
            },
            {
              key: 'public',
              label: '스터디 둘러보기',
              panel: (
                <StudyPanel key="public">
                  <PublicStudies />
                </StudyPanel>
              ),
            },
          ]}
        />
      </Main>
      <StudyFloatingActions />
    </Page>
  );
}

function StudyPanel({ children }: { children: ReactNode }) {
  return (
    <ErrorBoundary
      fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
    >
      <Suspense fallback={<Loading />}>{children}</Suspense>
    </ErrorBoundary>
  );
}
