import { CSSProperties, ReactNode } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import { useSuspenseQuery } from '@tanstack/react-query';
import { tokens, typography } from '../../../styles/global';
import List from '../../../shared/ui/List';
import ErrorContent from '../../../shared/ui/ErrorContent';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { memberQueries } from '../queries';
import MemberRow from './MemberRow';

const memberListSectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  flex: '0 1 auto',
  minHeight: 0,
} satisfies CSSProperties;

const memberListStyle = {
  flex: '0 1 auto',
  minHeight: 0,
  overflowY: 'auto',
} satisfies CSSProperties;

function MemberListSection({ children }: { children: ReactNode }) {
  return (
    <section css={memberListSectionStyle}>
      <h2 css={{ ...typography.subtitle, flexShrink: 0 }}>스터디 멤버</h2>
      <ErrorBoundary
        fallbackRender={({ error }) => (
          <div css={memberListStyle}>
            <ErrorContent message={getErrorMessage(error)} />
          </div>
        )}
      >
        {children}
      </ErrorBoundary>
    </section>
  );
}

interface MemberCountProps {
  nowMemberCount: number;
  maxMemberCount: number;
}

function MemberCount({ nowMemberCount, maxMemberCount }: MemberCountProps) {
  return (
    <p
      css={{
        ...typography.paragraph,
        flexShrink: 0,
        textAlign: 'right',
        margin: `${tokens.spacing[2]} 0`,
      }}
    >
      {nowMemberCount} / {maxMemberCount}
    </p>
  );
}

export function MemberListSectionForLeader() {
  return (
    <MemberListSection>
      <LeaderMemberList />
    </MemberListSection>
  );
}

export function MemberListSectionForMember() {
  return (
    <MemberListSection>
      <MemberList />
    </MemberListSection>
  );
}

function LeaderMemberList() {
  const { studyId } = useIntegerParams(['studyId']);
  const {
    data: { maxMemberCount, nowMemberCount, members },
  } = useSuspenseQuery(memberQueries.list(studyId));

  return (
    <>
      <List css={memberListStyle}>
        {members.map((member) => (
          <List.Item key={member.id}>
            <MemberRow.Leader data-testid="member-row" studyId={studyId} member={member} />
          </List.Item>
        ))}
      </List>
      <MemberCount maxMemberCount={maxMemberCount} nowMemberCount={nowMemberCount} />
    </>
  );
}

function MemberList() {
  const { studyId } = useIntegerParams(['studyId']);
  const {
    data: { maxMemberCount, nowMemberCount, members },
  } = useSuspenseQuery(memberQueries.list(studyId));

  return (
    <>
      <List css={memberListStyle}>
        {members.map((member) => (
          <List.Item key={member.id}>
            <MemberRow.Member data-testid="member-row" member={member} />
          </List.Item>
        ))}
      </List>
      <MemberCount maxMemberCount={maxMemberCount} nowMemberCount={nowMemberCount} />
    </>
  );
}
