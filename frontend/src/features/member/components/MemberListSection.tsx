import { CSSProperties } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { typography } from '../../../styles/global';
import List from '../../../shared/ui/List';
import ErrorContent from '../../../shared/ui/ErrorContent';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { memberQueries } from '../queries';
import MemberRow from './MemberRow';
import { Role } from '../../study/types';

interface MemberListSectionFallback {
  message?: string;
}

interface MemberListSectionProps {
  role: Role;
}

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

const SectionHeaderStyle = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
} satisfies CSSProperties;

function Heading() {
  return <h2 css={{ ...typography.subtitle, flexShrink: 0 }}>스터디 멤버</h2>;
}

export function MemberListSection({ role }: MemberListSectionProps) {
  const { studyId } = useIntegerParams(['studyId']);
  const {
    data: { maxMemberCount, nowMemberCount, members },
  } = useSuspenseQuery(memberQueries.list(studyId));

  return (
    <section css={memberListSectionStyle}>
      <div css={SectionHeaderStyle}>
        <Heading />
        <p css={typography.paragraph}>
          {nowMemberCount} / {maxMemberCount}
        </p>
      </div>
      <List css={memberListStyle}>
        {members.map((member) => (
          <List.Item key={member.id}>
            {role === 'LEADER' ? (
              <MemberRow.Leader data-testid="member-row" studyId={studyId} member={member} />
            ) : (
              <MemberRow.Member data-testid="member-row" member={member} />
            )}
          </List.Item>
        ))}
      </List>
    </section>
  );
}

export function MemberListSectionFallback({ message }: MemberListSectionFallback) {
  return (
    <section css={memberListSectionStyle}>
      <Heading />
      <ErrorContent message={message ?? '멤버 목록을 불러오는데 실패했습니다'} />
    </section>
  );
}
