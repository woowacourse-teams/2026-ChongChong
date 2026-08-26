import { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';
import noticeIcon from '../../../shared/assets/notice-green.svg';
import assignmentIcon from '../../../shared/assets/assign-green.svg';
import List from '../../../shared/ui/List';
import {
  LeaderActiveNoticeCard,
  LeaderActiveAssignmentCard,
  MemberActiveNoticeCard,
  MemberActiveAssignmentCard,
} from './ActiveStudyCard';
import { StudyLeaderWelcomeBanner, StudyMemberWelcomeBanner } from './WelcomeBanner';

const StatusCardListStyle = {
  display: 'flex',
  gap: tokens.spacing[3],
  marginBottom: tokens.spacing[5],
} satisfies CSSProperties;

const SectionLabelStyle = {
  ...typography.sectionLabel,
  margin: `0 0 ${tokens.spacing[3]}`,
  color: tokens.text.default,
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

export function LeaderStudyDetailContent({ username }: { username: string }) {
  const data = {
    notices: {
      count: 2,
      items: [
        {
          id: 1,
          title: '판교 스터디룸에서 만나도록 합시다',
          memberCount: 2,
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
          memberCount: 2,
          completeCount: 2,
        },
      ],
    },
  };
  return (
    <>
      <StudyLeaderWelcomeBanner username={username} />
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
              <LeaderActiveNoticeCard
                title={notice.title}
                memberCount={notice.memberCount}
                completeCount={notice.completeCount}
              />
            </List.Item>
          ))}
          {data.assignments.items.map((assignment) => (
            <List.Item key={`assignment-${assignment.id}`}>
              <LeaderActiveAssignmentCard
                title={assignment.title}
                memberCount={assignment.memberCount}
                completeCount={assignment.completeCount}
              />
            </List.Item>
          ))}
        </List>
      </section>
    </>
  );
}

export function MemberStudyDetailContent({ username }: { username: string }) {
  const data = {
    totalCount: 4,
    notices: [
      {
        id: 1,
        title: '판교 스터디룸에서 만나도록 합시다',
      },
      {
        id: 2,
        title: '어제 공지 읽었나요 ?',
      },
    ],
    assignments: [
      {
        id: 1,
        title: '그리디 3문제 풀기',
      },
    ],
  };
  const todoCount = data.notices.length + data.assignments.length;

  return (
    <div>
      <StudyMemberWelcomeBanner username={username} todoCount={todoCount} />
      <div css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[6] }}>
        <section>
          <h2 css={SectionLabelStyle}>읽지 않은 공지</h2>
          <List>
            {data.notices.map((notice) => (
              <List.Item key={`notice-${notice.id}`}>
                <MemberActiveNoticeCard title={notice.title} />
              </List.Item>
            ))}
          </List>
        </section>
        <section>
          <h2 css={SectionLabelStyle}>제출하지 않은 과제</h2>
          <List>
            {data.assignments.map((assignment) => (
              <List.Item key={`assignment-${assignment.id}`}>
                <MemberActiveAssignmentCard title={assignment.title} />
              </List.Item>
            ))}
          </List>
        </section>
      </div>
    </div>
  );
}
