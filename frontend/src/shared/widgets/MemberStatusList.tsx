import type { CSSProperties } from 'react';
import profileIcon from '../assets/unknown-profile.svg';
import List from '../ui/List';
import { tokens, typography } from '../../styles/global';

interface Member {
  id: number;
  name: string;
  profileImage: string | null;
}

interface Props {
  title: string;
  members: Member[];
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

const nameStyle = {
  ...typography.body,
  minWidth: 0,
  marginLeft: tokens.spacing[3],
  overflow: 'hidden',
  color: tokens.text.primary,
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

export default function MemberStatusList({ title, members }: Props) {
  return (
    <section css={sectionStyle} aria-label={title}>
      <h2 css={titleStyle}>{title}</h2>
      <List>
        {members.map((member) => (
          <List.Item key={member.id} css={itemStyle}>
            <img
              src={member.profileImage ?? profileIcon}
              alt=""
              aria-hidden="true"
              css={profileStyle}
            />
            <span css={nameStyle}>{member.name}</span>
          </List.Item>
        ))}
      </List>
    </section>
  );
}
