import { CSSProperties } from 'react';
import backIcon from '../../shared/assets/left-arrow.svg';
import alarmIcon from '../../shared/assets/alarm.svg';
import { tokens } from '../../styles/global';
import Button from '../../shared/ui/Button';
import TopHeader from '../../shared/ui/TopHeader';
import NoticeList from './components/NoticeList';

const pageStyle = {
  display: 'flex',
  flexDirection: 'column',
  minHeight: '100dvh',
  background: tokens.bg.default,
} satisfies CSSProperties;

const contentStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  padding: `${tokens.spacing[4]} ${tokens.layout.gutter} ${tokens.spacing[5]}`,
} satisfies CSSProperties;

const buttonAreaStyle = {
  display: 'flex',
  justifyContent: 'center',
  marginTop: tokens.spacing[3],
} satisfies CSSProperties;

export default function NoticeListPage() {
  const isLeader = true;

  return (
    <div css={pageStyle}>
      <TopHeader
        left={<img src={backIcon} alt="뒤로 가기" />}
        middle={
          <>
            <TopHeader.Title>우테코 8기 FE 스터디</TopHeader.Title>
            <TopHeader.Subtitle>디움 · 리드</TopHeader.Subtitle>
          </>
        }
        right={<img src={alarmIcon} alt="알림" />}
      />

      <main css={contentStyle}>
        <NoticeList />

        {isLeader && (
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
