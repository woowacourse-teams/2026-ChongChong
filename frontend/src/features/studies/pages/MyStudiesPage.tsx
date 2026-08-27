import { Suspense, CSSProperties } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import TopHeader from '../../../shared/ui/TopHeader';
import { Link } from 'react-router';
import logo from '../../../shared/assets/icons/header-icon.svg';
import MyStudies from '../components/MyStudies';
import Main from '../../../shared/ui/Main';
import { tokens } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import Page from '../../../shared/ui/Page';
import Loading from '../../../shared/ui/Loading';

const actionsStyle = {
  display: 'flex',
  gap: tokens.spacing[3],
  flexDirection: 'column',
  margin: `${tokens.spacing[3]} 0`,
} satisfies CSSProperties;

export default function MyStudiesPage() {
  return (
    <Page>
      <TopHeader
        middle={
          <div>
            <img css={{ width: '40px', height: '40px' }} src={logo} alt="" />
          </div>
        }
      />

      <Main>
        <ErrorBoundary fallbackRender={({ error }) => <p>{getErrorMessage(error)}</p>}>
          <Suspense fallback={<Loading />}>
            <MyStudies />

            <div css={actionsStyle}>
              {/* TODO: 진짜 link로 전환하는게 접근성 더 좋음, 스크린 리더 경험을 고려합니다. */}
              <Link to="/studies/new">
                <Button variant="brandSolid" size="large">
                  스터디 만들기
                </Button>
              </Link>
              <Link to="/studies/join">
                <Button role="link" variant="neutralOutline" size="large">
                  스터디 참여하기
                </Button>
              </Link>
            </div>
          </Suspense>
        </ErrorBoundary>
      </Main>
    </Page>
  );
}
