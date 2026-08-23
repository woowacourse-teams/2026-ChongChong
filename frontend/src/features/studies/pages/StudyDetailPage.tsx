import { CSSProperties } from 'react';
import Main from '../../../shared/ui/Main';
import BottomTab from '../../../shared/ui/components/BottomTab';
import TopHeader from '../../../shared/ui/TopHeader';
import Page from '../../../shared/ui/Page';
import List from '../../../shared/ui/List';
import { ActiveNoticeCard, ActiveAssignmentCard } from '../components/ActiveStudyCard';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import BannerLogo from '../../../shared/assets/icons/laptop-icon.svg';
import { tokens, typography } from '../../../styles/global';
import noticeIcon from '../../../shared/assets/notice-green.svg';
import assignmentIcon from '../../../shared/assets/assign-green.svg';

const BannerStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: tokens.spacing[3],
  padding: tokens.spacing[5],
  background: tokens.bg.brand,
  borderRadius: tokens.radius.lg,
  marginBottom: tokens.spacing[8],
} satisfies CSSProperties;

const BannerTextStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[2],
  minWidth: 0,
} satisfies CSSProperties;

const BannerTitleStyle = {
  ...typography.title,
  margin: 0,
  color: tokens.text.onBrand,
  fontWeight: tokens.fontWeight.semibold,
} satisfies CSSProperties;

const BannerDescriptionStyle = {
  ...typography.body,
  margin: 0,
  color: tokens.text.onBrand,
} satisfies CSSProperties;

const SectionLabelStyle = {
  ...typography.sectionLabel,
  margin: `0 0 ${tokens.spacing[3]}`,
  color: tokens.text.default,
} satisfies CSSProperties;

const StatusCardListStyle = {
  display: 'flex',
  gap: tokens.spacing[3],
  marginBottom: tokens.spacing[5],
} satisfies CSSProperties;

const StatusCardStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[1],
  padding: tokens.spacing[4],
  background: tokens.bg.default,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
  boxShadow: tokens.shadow[1],
} satisfies CSSProperties;

const StatusCountStyle = {
  ...typography.headline,
  margin: 0,
  color: tokens.text.default,
} satisfies CSSProperties;

const StatusLabelStyle = {
  ...typography.body,
  margin: 0,
  color: tokens.text.default,
} satisfies CSSProperties;

const IconStyle = {
  width: '20px',
  height: '20px',
} satisfies CSSProperties;

const data = {
  memberCount: 4,
  notices: {
    count: 2,
    items: [
      {
        id: 1,
        title: '판교 스터디룸에서 만나도록 합시다',
        completeCount: 2,
      },
    ],
  },
  assignments: {
    count: 1,
    items: [
      {
        id: 1,
        title: '그리디 3문제 풀기',
        completeCount: 2,
      },
    ],
  },
};

export default function StudyDetailPage() {
  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={
          <>
            <TopHeader.Title>우테코 8기 FE 스터디</TopHeader.Title>
            <TopHeader.Subtitle>바니 · 리드</TopHeader.Subtitle>
          </>
        }
      />
      <Main>
        <div css={BannerStyle}>
          <div css={BannerTextStyle}>
            <p css={BannerTitleStyle}>바니님, 오늘도 화이팅!</p>
            <p css={BannerDescriptionStyle}>리마인드는 총총이 대신 보낼게요</p>
          </div>
          <img src={BannerLogo} alt="" css={{ width: '120px', height: '120px', flexShrink: 0 }} />
        </div>
        <section>
          <h2 css={SectionLabelStyle}>스터디 현황</h2>
          <div css={StatusCardListStyle}>
            <div css={StatusCardStyle}>
              <img src={noticeIcon} alt="" css={IconStyle} />
              <p css={StatusCountStyle}>{data.notices.count}</p>
              <p css={StatusLabelStyle}>안내 중인 공지</p>
            </div>
            <div css={StatusCardStyle}>
              <img src={assignmentIcon} alt="" css={IconStyle} />
              <p css={StatusCountStyle}>{data.assignments.count}</p>
              <p css={StatusLabelStyle}>진행 중인 과제</p>
            </div>
          </div>
          <List aria-label="진행 중인 공지와 과제">
            {data.notices.items.map((notice) => (
              <List.Item key={`notice-${notice.id}`}>
                <ActiveNoticeCard
                  title={notice.title}
                  memberCount={data.memberCount}
                  completeCount={notice.completeCount}
                />
              </List.Item>
            ))}
            {data.assignments.items.map((assignment) => (
              <List.Item key={`assignment-${assignment.id}`}>
                <ActiveAssignmentCard
                  title={assignment.title}
                  memberCount={data.memberCount}
                  completeCount={assignment.completeCount}
                />
              </List.Item>
            ))}
          </List>
        </section>
      </Main>
      <BottomTab />
    </Page>
  );
}
