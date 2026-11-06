import type { ComponentPropsWithoutRef } from 'react';
import type { CSSObject } from '@emotion/react';
import { tokens } from '../../../styles/global';
import checkedIcon from '../../assets/checkbox-checked.svg';

const checkboxStyle = {
  appearance: 'none',
  width: 20,
  height: 20,
  flexShrink: 0,
  margin: 0,
  border: `2px solid ${tokens.color.optionGrayBackgroundIcon}`,
  borderRadius: 4,
  cursor: 'pointer',
  '&:checked': { background: `url(${checkedIcon}) center / 20px 20px no-repeat`, border: 0 },
  '&:focus-visible': { outline: `2px solid ${tokens.text.primary}`, outlineOffset: 2 },
} satisfies CSSObject;

export default function Checkbox(props: ComponentPropsWithoutRef<'input'>) {
  return <input {...props} type="checkbox" css={checkboxStyle} />;
}
