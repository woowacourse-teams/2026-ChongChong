import { tokens, typography } from '../../../styles/global';

interface LabelProps {
  text: string;
  isRequired: boolean;
}

export default function Label({ text, isRequired }: LabelProps) {
  return (
    <label css={{ ...typography.sectionLabel, color: tokens.text.primary, marginBottom: '4px' }}>
      {text} {isRequired && <span css={{ color: tokens.text.brand }}>*</span>}
    </label>
  );
}
