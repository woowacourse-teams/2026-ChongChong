import { Suspense, CSSProperties } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import TopHeader from '../../../shared/ui/TopHeader';
import { Link } from 'react-router';
import logo from '../../../shared/assets/icons/header-icon.svg';
import MyStudies from '../components/MyStudies';
import Main from '../../../shared/ui/Main';
import footerIcon from '../../../shared/assets/icons/footer-icon.svg';
import { tokens } from '../../../styles/global';
import { typography } from '../../../styles/global';
import Button from '../../../shared/ui/Button';
import Page from '../../../shared/ui/Page';

const actionsStyle = {
  display: 'flex',
  gap: tokens.spacing[3],
  flexDirection: 'column',
  margin: `${tokens.spacing[3]} 0`,
} satisfies CSSProperties;

const footerBannerStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: tokens.spacing[1],
} satisfies CSSProperties;

export default function StudyListPage() {
  return (
    <Page>
      <TopHeader
        middle={
          <div>
            <img css={{ width: '40px', height: '40px' }} src={logo} alt="" />
          </div>
        }
        right={<a href="#">My</a>}
      />
      <Main>
        <ErrorBoundary fallbackRender={({ error }) => <p>{getErrorMessage(error)}</p>}>
          <Suspense fallback={<p>loading ...</p>}>
            <MyStudies />
          </Suspense>
        </ErrorBoundary>
        <div css={actionsStyle}>
          {/* 진짜 link로 전환하는게 접근성 더 좋음, 스크린 리더의 링크에 안잡힘 */}
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
        <aside css={footerBannerStyle}>
          <img src={footerIcon} css={{ height: '52px', width: '52px' }} alt="" />
          <div css={{ display: 'flex', gap: tokens.spacing[1], flexDirection: 'column' }}>
            <p css={typography.paragraph}>리마인드는 총총이 보낼게요.</p>
            <p css={{ ...typography.footnote, color: tokens.color.optionSubFontColor55 }}>
              정해둔 시각에 미확인자, 미제출자에게 알림을 보내요
            </p>
          </div>
        </aside>
      </Main>
    </Page>
  );
}
