import { CSSProperties } from 'react';
import { tokens } from '../../styles/global';

const mainStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  padding: `${tokens.spacing[4]} ${tokens.layout.gutter} ${tokens.spacing[5]}`,
} satisfies CSSProperties;

export default function Main({ children, ...props }: React.ComponentProps<'main'>) {
  return (
    <main css={mainStyle} {...props}>
      {children}
    </main>
  );
}
