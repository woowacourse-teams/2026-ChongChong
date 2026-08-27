import type { CSSProperties } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import backIcon from '../../../shared/assets/left-arrow.svg';
import Button from '../../../shared/ui/Button';
import EmptyContent from '../../../shared/ui/EmptyContent';
import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import { tokens } from '../../../styles/global';
import LeaderNoticeList from '../components/LeaderNoticeList';
import { notices as allNotices } from '../noticeData';
import Page from '../../../shared/ui/Page';
import BottomTab from '../../../shared/ui/components/BottomTab';
import { Suspense } from 'react';
import Loading from '../../../shared/ui/Loading';

const emptyContentStyle = {
  alignItems: 'center',
  justifyContent: 'center',
} satisfies CSSProperties;

const buttonAreaStyle = {
  display: 'flex',
  width: '100%',
  justifyContent: 'center',
  marginTop: tokens.spacing[3],
} satisfies CSSProperties;

const iconButtonStyle = {
  display: 'grid',
  width: '32px',
  height: '32px',
  padding: 0,
  placeItems: 'center',
  border: 0,
  background: 'transparent',
  cursor: 'pointer',
} satisfies CSSProperties;

export default function LeaderNoticeListPage() {
  const navigate = useNavigate();
  const { studyId = '1' } = useParams();
  const [searchParams] = useSearchParams();
  const notices = searchParams.get('empty') === 'true' ? [] : allNotices;

  return (
    <Page>
      <TopHeader
        left={
          <button
            type="button"
            css={iconButtonStyle}
            aria-label="뒤로 가기"
            onClick={() => navigate(-1)}
          >
            <img src={backIcon} alt="뒤로 가기" width={24} height={24} />
          </button>
        }
        middle={
          <>
            <TopHeader.Title>우테코 8기 FE 스터디</TopHeader.Title>
            <TopHeader.Subtitle>디움 · 리드</TopHeader.Subtitle>
          </>
        }
      />
      <Suspense fallback={<Loading />}>
        <Main css={{ ...(notices.length === 0 ? emptyContentStyle : {}) }}>
          {notices.length === 0 ? (
            <EmptyContent message="아직 공지가 없어요" />
          ) : (
            <LeaderNoticeList notices={notices} studyId={studyId} />
          )}

          <div css={buttonAreaStyle}>
            <Button
              variant="brandSolid"
              size="large"
              onClick={() => navigate(`/studies/${studyId}/notices/create`)}
            >
              공지 작성하기
            </Button>
          </div>
        </Main>
      </Suspense>
      <BottomTab />
    </Page>
  );
}
