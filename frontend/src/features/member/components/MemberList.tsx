import { CSSProperties } from 'react';
import { useSuspenseQuery } from '@tanstack/react-query';
import { tokens } from '../../../styles/global';
import List from '../../../shared/ui/List';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { memberQueries } from '../queries';
import MemberRow from './MemberRow';

const listStyle = {
  marginBottom: tokens.spacing[6],
} satisfies CSSProperties;

function LeaderListView() {
  const { studyId } = useIntegerParams(['studyId']);
  const {
    data: { members },
  } = useSuspenseQuery(memberQueries.list(studyId));

  return (
    <List css={listStyle}>
      {members.map((member) => (
        <List.Item key={member.id}>
          <MemberRow.Leader data-testid="member-row" studyId={studyId} member={member} />
        </List.Item>
      ))}
    </List>
  );
}

function MemberListView() {
  const { studyId } = useIntegerParams(['studyId']);
  const {
    data: { members },
  } = useSuspenseQuery(memberQueries.list(studyId));

  return (
    <List css={listStyle}>
      {members.map((member) => (
        <List.Item key={member.id}>
          <MemberRow.Member data-testid="member-row" member={member} />
        </List.Item>
      ))}
    </List>
  );
}

const MemberList = {
  Leader: LeaderListView,
  Member: MemberListView,
};

export default MemberList;
