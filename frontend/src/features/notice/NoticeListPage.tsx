import type { CSSProperties } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router';
import alarmIcon from '../../shared/assets/alarm.svg';
import backIcon from '../../shared/assets/left-arrow.svg';
import Button from '../../shared/ui/Button';
import EmptyState from '../../shared/ui/EmptyState';
import TopHeader from '../../shared/ui/TopHeader';
import { tokens } from '../../styles/global';
import NoticeList, { type NoticeListItem } from './components/NoticeList';

const pageStyle = {
  display: 'flex',
  minHeight: '100dvh',
  flexDirection: 'column',
  background: tokens.bg.default,
} satisfies CSSProperties;

const contentStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  padding: `${tokens.spacing[4]} ${tokens.layout.gutter} ${tokens.spacing[5]}`,
} satisfies CSSProperties;

const emptyContentStyle = {
  alignItems: 'center',
  justifyContent: 'center',
  paddingBottom: '84px',
} satisfies CSSProperties;

const buttonAreaStyle = {
  display: 'flex',
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

const memberNotices: NoticeListItem[] = [
  {
    id: 1,
    title: '8월 스터디 운영 방식이 바뀝니다',
    description: '8월부터 스터디 운영 방식을 조금 바꾸려고 합니다.',
    createdAt: '5시간 전',
    isRead: false,
    readCount: 2,
    totalCount: 4,
    reminderText: '1분 뒤 리마인드',
  },
  {
    id: 2,
    title: '다음 주 스터디는 온라인으로 진행합니다',
    description: '장소 대관 일정으로 인해 다음 주는 온라인으로 만나요.',
    createdAt: '8월 1일',
    isRead: true,
    readCount: 4,
    totalCount: 4,
  },
];

export default function NoticeListPage() {
  const navigate = useNavigate();
  const { studyId = '1' } = useParams();
  const [searchParams] = useSearchParams();
  const isLeader = searchParams.get('role') === 'leader';
  const notices = searchParams.get('empty') === 'true' ? [] : memberNotices;

  return (
    <div css={pageStyle}>
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
            <TopHeader.Subtitle>{isLeader ? '디움 · 리드' : '바니 · 스터디원'}</TopHeader.Subtitle>
          </>
        }
        right={<img src={alarmIcon} alt="알림" width={24} height={24} />}
      />

      <main css={{ ...contentStyle, ...(notices.length === 0 ? emptyContentStyle : {}) }}>
        {notices.length === 0 ? (
          <EmptyState message="아직 공지가 없어요" />
        ) : (
          <NoticeList
            notices={notices}
            isLeader={isLeader}
            onSelect={(noticeId) => navigate(`/studies/${studyId}/notices/${noticeId}`)}
          />
        )}

        {isLeader && notices.length > 0 && (
          <div css={buttonAreaStyle}>
            <Button variant="brandSolid" size="large">
              공지 작성하기
            </Button>
          </div>
        )}
      </main>
    </div>
  );
}
