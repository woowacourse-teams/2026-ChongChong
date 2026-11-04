import { Link } from 'react-router';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { CSSProperties, useMemo } from 'react';
import { useSuspenseQueries, useSuspenseQuery } from '@tanstack/react-query';
import { tokens } from '../../../styles/global';
import { Heading, Paragraph } from '../../../shared/ui/Typography';
import noticeIcon from '../../../shared/assets/notice-green.webp';
import assignmentIcon from '../../../shared/assets/assign-green.webp';
import List from '../../../shared/ui/List';
import studyQueries from '../queries';
import {
  LeaderActiveNoticeCard,
  LeaderActiveAssignmentCard,
  MemberActiveNoticeCard,
  MemberActiveAssignmentCard,
} from './ActiveStudyCard';
import { CheeringBanner, TodoBanner, InviteMemberBanner } from './Banner';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import SleepIcon from '../../../shared/assets/icons/sleep-icon.webp';
import WritingLogo from '../../../shared/assets/icons/writing-logo.webp';
import { memberQueries } from '../../member/queries';

const StatusCardListStyle = {
  display: 'flex',
  gap: tokens.spacing[3],
  marginBottom: tokens.spacing[5],
} satisfies CSSProperties;

const SectionLabelStyle = {
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
  margin: 0,
  color: tokens.text.default,
} satisfies CSSProperties;

const StatusLabelStyle = {
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
  margin: 0,
  color: tokens.text.default,
  fontWeight: tokens.fontWeight.semibold,
} satisfies CSSProperties;

const CompletedDescriptionStyle = {
  margin: 0,
  color: tokens.text.muted,
} satisfies CSSProperties;

const IconStyle = {
  width: '20px',
  height: '20px',
} satisfies CSSProperties;

export function LeaderStudyDetailContent({ username }: { username: string }) {
  const { studyId } = useIntegerParams(['studyId']);
  const [
    {
      data: { notices, assignments },
    },
    {
      data: { nowMemberCount },
    },
  ] = useSuspenseQueries({
    queries: [studyQueries.detail(studyId, 'LEADER'), memberQueries.list(studyId)],
  });

  const activeContentCount = useMemo(
    () => notices.count + assignments.count,
    [notices, assignments],
  );

  return (
    <div css={DetailContentStyle}>
      {nowMemberCount === 1 ? (
        <InviteMemberBanner studyId={studyId} invitePageLink={URL_POLICY.toMembers(studyId).to} />
      ) : (
        <CheeringBanner username={username} />
      )}
      <section css={SectionStyle}>
        <Heading variant="sectionLabel" level={2} css={SectionLabelStyle}>
          스터디 현황
        </Heading>
        <div css={StatusCardListStyle}>
          <Link {...URL_POLICY.toNotices(studyId)} css={StatusCardStyle}>
            <div>
              <img src={noticeIcon} alt="" css={IconStyle} />
              <Paragraph variant="headline" css={StatusCountStyle}>
                {notices.count}
              </Paragraph>
              <Paragraph variant="body" css={StatusLabelStyle}>
                안내 중인 공지
              </Paragraph>
            </div>
          </Link>
          <Link {...URL_POLICY.toAssignments(studyId)} css={StatusCardStyle}>
            <div>
              <img src={assignmentIcon} alt="" css={IconStyle} />
              <Paragraph variant="headline" css={StatusCountStyle}>
                {assignments.count}
              </Paragraph>
              <Paragraph variant="body" css={StatusLabelStyle}>
                진행 중인 과제
              </Paragraph>
            </div>
          </Link>
        </div>
        {activeContentCount === 0 ? (
          <div css={CompletedContentStyle}>
            <img src={WritingLogo} alt="" css={CompletedImageStyle} />
            <Heading variant="title" level={2} css={CompletedTitleStyle}>
              아직 진행 중인 공지나 과제가 없어요!
            </Heading>
            <Paragraph variant="body" css={CompletedDescriptionStyle}>
              새로운 공지나 과제를 작성하러 가볼까요?
            </Paragraph>
          </div>
        ) : (
          <List aria-label="진행 중인 공지와 과제">
            {notices.items.map((notice) => (
              <List.Item key={`notice-${notice.id}`}>
                <Link {...URL_POLICY.toNotice(studyId, notice.id)}>
                  <LeaderActiveNoticeCard
                    title={notice.title}
                    memberCount={notice.memberCount}
                    completeCount={notice.completeCount}
                  />
                </Link>
              </List.Item>
            ))}
            {assignments.items.map((assignment) => (
              <List.Item key={`assignment-${assignment.id}`}>
                <Link {...URL_POLICY.toAssignment(studyId, assignment.id)}>
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
  const { studyId } = useIntegerParams(['studyId']);
  const { data } = useSuspenseQuery(studyQueries.detail(studyId, 'MEMBER'));
  const todoCount = data.totalCount;

  return (
    <div css={DetailContentStyle}>
      <TodoBanner username={username} todoCount={todoCount} />
      <div css={SectionListStyle}>
        {todoCount === 0 ? (
          <section css={CompletedContentStyle}>
            <img src={SleepIcon} alt="" css={CompletedImageStyle} />
            <Heading variant="title" level={2} css={CompletedTitleStyle}>
              오늘 할 일을 모두 마쳤어요!
            </Heading>
            <Paragraph variant="body" css={CompletedDescriptionStyle}>
              모든 공지를 확인했고, 제출할 과제도 없어요.
              <br />
              잠깐 쉬어가는건 어떨까요 ?
            </Paragraph>
          </section>
        ) : (
          <>
            {data.notices.items.length !== 0 && (
              <section css={SectionStyle}>
                <Heading variant="sectionLabel" level={2} css={SectionLabelStyle}>
                  읽지 않은 공지
                </Heading>
                <List>
                  {data.notices.items.map((notice) => (
                    <List.Item key={`notice-${notice.id}`}>
                      <Link {...URL_POLICY.toNotice(studyId, notice.id)}>
                        <MemberActiveNoticeCard title={notice.title} />
                      </Link>
                    </List.Item>
                  ))}
                </List>
              </section>
            )}
            {data.assignments.items.length !== 0 && (
              <section css={SectionStyle}>
                <Heading variant="sectionLabel" level={2} css={SectionLabelStyle}>
                  제출하지 않은 과제
                </Heading>
                <List>
                  {data.assignments.items.map((assignment) => (
                    <List.Item key={`assignment-${assignment.id}`}>
                      <Link {...URL_POLICY.toAssignment(studyId, assignment.id)}>
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
