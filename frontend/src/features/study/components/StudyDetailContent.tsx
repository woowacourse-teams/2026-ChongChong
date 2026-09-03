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
import SleepIcon from '../../../shared/assets/icons/sleep-icon.webp';
import WritingLogo from '../../../shared/assets/icons/writing-logo.webp';

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

const DetailContentStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

const SectionListStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[6],
  minHeight: 0,
} satisfies CSSProperties;

const SectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  minHeight: 0,
} satisfies CSSProperties;

const CompletedContentStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  justifyContent: 'center',
  alignItems: 'center',
  gap: tokens.spacing[2],
  paddingBottom: tokens.spacing[8],
  textAlign: 'center',
} satisfies CSSProperties;

const CompletedImageStyle = {
  width: 'min(100%, 300px)',
  height: 'auto',
} satisfies CSSProperties;

const CompletedTitleStyle = {
  ...typography.title,
  margin: 0,
  color: tokens.text.default,
  fontWeight: tokens.fontWeight.semibold,
} satisfies CSSProperties;

const CompletedDescriptionStyle = {
  ...typography.body,
  margin: 0,
  color: tokens.text.muted,
} satisfies CSSProperties;

const IconStyle = {
  width: '20px',
  height: '20px',
} satisfies CSSProperties;

export function LeaderStudyDetailContent({ username }: { username: string }) {
  const { studyId } = useStudyId();
  const { data } = useSuspenseQuery(studyQueries.detail(studyId, 'LEADER'));
  const activeContentCount = data.notices.count + data.assignments.count;

  return (
    <div css={DetailContentStyle}>
      <StudyLeaderWelcomeBanner username={username} />
      <section css={SectionStyle}>
        <h2 css={SectionLabelStyle}>스터디 현황</h2>
        <div css={StatusCardListStyle}>
          <Link to="notices" css={StatusCardStyle}>
            <div>
              <img src={noticeIcon} alt="" css={IconStyle} />
              <p css={StatusCountStyle}>{data.notices.count}</p>
              <p css={StatusLabelStyle}>안내 중인 공지</p>
            </div>
          </Link>
          <Link to="assignments" css={StatusCardStyle}>
            <div>
              <img src={assignmentIcon} alt="" css={IconStyle} />
              <p css={StatusCountStyle}>{data.assignments.count}</p>
              <p css={StatusLabelStyle}>진행 중인 과제</p>
            </div>
          </Link>
        </div>
        {activeContentCount === 0 ? (
          <div css={CompletedContentStyle}>
            <img src={WritingLogo} alt="" css={CompletedImageStyle} />
            <h2 css={CompletedTitleStyle}>아직 진행 중인 공지나 과제가 없어요!</h2>
            <p css={CompletedDescriptionStyle}>새로운 공지나 과제를 작성하러 가볼까요?</p>
          </div>
        ) : (
          <List aria-label="진행 중인 공지와 과제">
            {data.notices.items.map((notice) => (
              <List.Item key={`notice-${notice.id}`}>
                <Link to={`notices/${notice.id}`}>
                  <LeaderActiveNoticeCard
                    title={notice.title}
                    memberCount={notice.memberCount}
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
                    memberCount={assignment.memberCount}
                    completeCount={assignment.completeCount}
                  />
                </Link>
              </List.Item>
            ))}
          </List>
        )}
      </section>
    </div>
  );
}

export function MemberStudyDetailContent({ username }: { username: string }) {
  const { studyId } = useStudyId();
  const { data } = useSuspenseQuery(studyQueries.detail(studyId, 'MEMBER'));
  const todoCount = data.totalCount;

  return (
    <div css={DetailContentStyle}>
      <StudyMemberWelcomeBanner username={username} todoCount={todoCount} />
      <div css={SectionListStyle}>
        {todoCount === 0 ? (
          <section css={CompletedContentStyle}>
            <img src={SleepIcon} alt="" css={CompletedImageStyle} />
            <h2 css={CompletedTitleStyle}>오늘 할 일을 모두 마쳤어요!</h2>
            <p css={CompletedDescriptionStyle}>
              모든 공지를 확인했고, 제출할 과제도 없어요.
              <br />
              잠깐 쉬어가는건 어떨까요 ?
            </p>
          </section>
        ) : (
          <>
            {data.notices.items.length !== 0 && (
              <section css={SectionStyle}>
                <h2 css={SectionLabelStyle}>읽지 않은 공지</h2>
                <List>
                  {data.notices.items.map((notice) => (
                    <List.Item key={`notice-${notice.id}`}>
                      <Link to={`notices/${notice.id}`}>
                        <MemberActiveNoticeCard title={notice.title} />
                      </Link>
                    </List.Item>
                  ))}
                </List>
              </section>
            )}
            {data.assignments.items.length !== 0 && (
              <section css={SectionStyle}>
                <h2 css={SectionLabelStyle}>제출하지 않은 과제</h2>
                <List>
                  {data.assignments.items.map((assignment) => (
                    <List.Item key={`assignment-${assignment.id}`}>
                      <Link to={`assignments/${assignment.id}`}>
                        <MemberActiveAssignmentCard title={assignment.title} />
                      </Link>
                    </List.Item>
                  ))}
                </List>
              </section>
            )}
          </>
        )}
      </div>
    </div>
  );
}
