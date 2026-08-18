import { CSSProperties, ComponentProps } from 'react';
import { tokens } from '../../styles/global';

type ListProps = ComponentProps<'ul'>;
type ListItemProps = ComponentProps<'li'>;

const listStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
  gap: tokens.spacing[3],
  margin: 0,
  padding: 0,
  listStyle: 'none',
} satisfies CSSProperties;

function ListRoot({ children, ...props }: ListProps) {
  return (
    <ul css={listStyle} {...props}>
      {children}
    </ul>
  );
}

function Item({ children, ...props }: ListItemProps) {
  return <li {...props}>{children}</li>;
}

const List = Object.assign(ListRoot, {
  Item,
});

export default List;
