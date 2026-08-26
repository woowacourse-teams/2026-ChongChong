import { CSSProperties } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { tokens, typography } from '../../../styles/global';
import noticeIcon from '../../../shared/assets/notice-green.svg';
import assignmentIcon from '../../../shared/assets/assign-green.svg';
import List from '../../../shared/ui/List';
import studyQueries from '../queries';
import {
  LeaderActiveNoticeCard,
  LeaderActiveAssignmentCard,
  MemberActiveNoticeCard,
  MemberActiveAssignmentCard,
} from './ActiveStudyCard';
import { StudyLeaderWelcomeBanner, StudyMemberWelcomeBanner } from './WelcomeBanner';
import useStudyId from '../hooks/useStudyId';

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
  const { studyId } = useStudyId();
  const { data } = useSuspenseQuery(studyQueries.detail<'LEADER'>(studyId));
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
              <Link to={`notices/${notice.id}`}>
                <LeaderActiveNoticeCard
                  title={notice.title}
                  memberCount={data.memberCount}
                  completeCount={notice.completeCount}
                />
              </Link>
            </List.Item>
          ))}
          {data.assignments.items.map((assignment) => (
            <List.Item key={`assignment-${assignment.id}`}>
              <Link to={`assignments/${assignment.id}`}>
                <LeaderActiveAssignmentCard
                  title={assignment.title}
                  memberCount={data.memberCount}
                  completeCount={assignment.completeCount}
                />
              </Link>
            </List.Item>
          ))}
        </List>
      </section>
    </>
  );
}

export function MemberStudyDetailContent({ username }: { username: string }) {
  const { studyId } = useStudyId();
  const { data } = useSuspenseQuery(studyQueries.detail<'MEMBER'>(studyId));
  const todoCount = data.totalCount;

  return (
    <div>
      <StudyMemberWelcomeBanner username={username} todoCount={todoCount} />
      <div css={{ display: 'flex', flexDirection: 'column', gap: tokens.spacing[6] }}>
        <section>
          <h2 css={SectionLabelStyle}>읽지 않은 공지</h2>
          <List>
            {data.notices.map((notice) => (
              <List.Item key={`notice-${notice.id}`}>
                <Link to={`notices/${notice.id}`}>
                  <MemberActiveNoticeCard title={notice.title} />
                </Link>
              </List.Item>
            ))}
          </List>
        </section>
        <section>
          <h2 css={SectionLabelStyle}>제출하지 않은 과제</h2>
          <List>
            {data.assignments.map((assignment) => (
              <List.Item key={`assignment-${assignment.id}`}>
                <Link to={`assignments/${assignment.id}`}>
                  <MemberActiveAssignmentCard title={assignment.title} />
                </Link>
              </List.Item>
            ))}
          </List>
        </section>
      </div>
    </div>
  );
}
