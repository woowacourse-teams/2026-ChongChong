import { tokens, typography } from '../../../styles/global';

interface InputProps {
  onChange: () => void;
  placeholder: string;
}

const inputStyle = {
  width: '100%',
  minHeight: '52px',
  padding: tokens.spacing[4],
  alignItems: 'flex-start',
  borderRadius: tokens.radius.md,
  ...typography.label,
  border: tokens.border.default,

  '&:focus': {
    border: tokens.border.brand,
    outline: 'none',
  },
};

export default function Input({ onChange, placeholder }: InputProps) {
  return (
    <input type="text" css={{ ...inputStyle }} onChange={onChange} placeholder={placeholder} />
  );
}
