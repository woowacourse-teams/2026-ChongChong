import type { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';
import List from '../../../shared/ui/List';

const menuItems = [
  { label: '로그아웃', color: tokens.text.critical },
  { label: '개인정보처리방침', color: tokens.color.mainBlack },
  { label: '지원', color: tokens.color.mainBlack },
  { label: '회원 탈퇴', color: tokens.text.muted },
] as const;

const sectionStyle = {
  marginTop: '200px',
} satisfies CSSProperties;

export default function AccountMenuSection() {
  return (
    <section css={sectionStyle} aria-label="계정 메뉴">
      <List>
        {menuItems.map(({ label, color }) => (
          <List.Item key={label} css={{ ...typography.title, color }}>
            {label}
          </List.Item>
        ))}
      </List>
    </section>
  );
}
