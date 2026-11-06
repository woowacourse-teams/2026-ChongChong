import type { CSSObject } from '@emotion/react';
import type { SubmissionVisibility } from '../../types';
import selectedIcon from '../../assets/visibility-selected.svg';
import unselectedIcon from '../../assets/visibility-unselected.svg';
import { Paragraph } from '../../../../shared/ui/Typography';
import { tokens } from '../../../../styles/global';

interface VisibilityOptionFieldProps {
  label: string;
  description: string;
  value: SubmissionVisibility;
  isActive: boolean;
  onChange: (visibility: SubmissionVisibility) => void;
}

const labelStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[2],
  padding: tokens.spacing[5],
  borderRadius: tokens.radius.md,
  cursor: 'pointer',
} satisfies CSSObject;

const inputStyle = {
  appearance: 'none',
  margin: 0,
  width: 24,
  height: 24,
  flexShrink: 0,
  cursor: 'pointer',
  '&:focus-visible': {
    outline: `2px solid ${tokens.text.primary}`,
    outlineOffset: 2,
  },
} satisfies CSSObject;

export default function VisibilityOptionField({
  label,
  description,
  value,
  onChange,
  isActive,
}: VisibilityOptionFieldProps) {
  return (
    <label
      css={{
        ...labelStyle,
        border: isActive ? `1.5px solid ${tokens.text.brand}` : tokens.border.default,
        background: isActive ? tokens.bg.brandSubtle : tokens.bg.default,
      }}
    >
      <span
        css={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: tokens.spacing[3],
        }}
      >
        <Paragraph variant="title">{label}</Paragraph>
        <input
          type="radio"
          name="submissionVisibility"
          aria-label={label}
          value={value}
          checked={isActive}
          onChange={() => onChange(value)}
          css={{
            ...inputStyle,
            background: `url(${isActive ? selectedIcon : unselectedIcon}) center / 24px 24px no-repeat`,
          }}
        />
      </span>
      <Paragraph variant="paragraph" css={{ whiteSpace: 'pre-line', color: tokens.text.secondary }}>
        {description}
      </Paragraph>
    </label>
  );
}
