import type { CSSProperties } from 'react';
import profileIcon from '../../../shared/assets/unknown-profile.svg';
import List from '../../../shared/ui/List';
import { formatReadAt } from '../../../shared/utils/formatDate';
import { tokens, typography } from '../../../styles/global';
import type { ReadNoticeMember } from '../types';

interface Props {
  members: ReadNoticeMember[];
}

const sectionStyle = {
  display: 'flex',
  flexDirection: 'column',
  marginTop: tokens.spacing[5],
} satisfies CSSProperties;

const titleStyle = {
  ...typography.title,
  margin: `0 0 ${tokens.spacing[3]}`,
  color: tokens.text.primary,
} satisfies CSSProperties;

const itemStyle = {
  display: 'flex',
  minHeight: 72,
  alignItems: 'center',
  padding: tokens.spacing[4],
  border: tokens.border.neutral,
  borderRadius: tokens.radius.md,
  background: tokens.bg.default,
} satisfies CSSProperties;

const profileStyle = {
  width: 28,
  height: 28,
  flex: '0 0 28px',
} satisfies CSSProperties;

const memberStyle = {
  display: 'flex',
  minWidth: 0,
  flexDirection: 'column',
  marginLeft: tokens.spacing[3],
} satisfies CSSProperties;

const nameStyle = {
  ...typography.body,
  overflow: 'hidden',
  color: tokens.text.primary,
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

const dateStyle = {
  ...typography.footnote,
  color: tokens.text.muted,
} satisfies CSSProperties;

export default function ReadMemberList({ members }: Props) {
  return (
    <section css={sectionStyle} aria-labelledby="read-member-list-title">
      <h2 id="read-member-list-title" css={titleStyle}>
        확인 {members.length}명
      </h2>
      <List>
        {members.map((member) => (
          <List.Item key={member.id} css={itemStyle}>
            <img
              src={member.profileImage ?? profileIcon}
              alt=""
              aria-hidden="true"
              css={profileStyle}
            />
            <div css={memberStyle}>
              <span css={nameStyle}>{member.name}</span>
              {member.readAt ? (
                <time css={dateStyle} dateTime={member.readAt}>
                  {formatReadAt(member.readAt)}
                </time>
              ) : null}
            </div>
          </List.Item>
        ))}
      </List>
    </section>
  );
}
