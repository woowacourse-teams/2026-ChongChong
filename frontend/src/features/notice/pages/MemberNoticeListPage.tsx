import type { CSSProperties } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import alarmIcon from '../../../shared/assets/alarm.svg';
import backIcon from '../../../shared/assets/left-arrow.svg';
import EmptyState from '../../../shared/ui/EmptyState';
import Main from '../../../shared/ui/Main';
import TopHeader from '../../../shared/ui/TopHeader';
import MemberNoticeList from '../components/MemberNoticeList';
import { notices as allNotices } from '../noticeData';
import Page from '../../../shared/ui/Page';

const emptyContentStyle = {
  alignItems: 'center',
  justifyContent: 'center',
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

export default function MemberNoticeListPage() {
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
            <TopHeader.Subtitle>바니 · 스터디원</TopHeader.Subtitle>
          </>
        }
        right={<img src={alarmIcon} alt="알림" width={24} height={24} />}
      />

      <Main css={{ ...(notices.length === 0 ? emptyContentStyle : {}) }}>
        {notices.length === 0 ? (
          <EmptyState message="아직 공지가 없어요" />
        ) : (
          <MemberNoticeList notices={notices} studyId={studyId} />
        )}
      </Main>
    </Page>
  );
}
