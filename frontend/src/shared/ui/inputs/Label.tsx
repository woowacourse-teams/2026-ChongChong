import { tokens, typography } from '../../../styles/global';

interface LabelProps {
  htmlFor: string;
  text: string;
  isRequired?: boolean;
}

export default function Label({ htmlFor, text, isRequired = false }: LabelProps) {
  return (
    <label
      htmlFor={htmlFor}
      css={{ ...typography.sectionLabel, color: tokens.text.primary, marginBottom: '4px' }}
    >
      {text}{' '}
      {isRequired && (
        <span aria-hidden="true" css={{ color: tokens.text.brand }}>
          *
        </span>
      )}
    </label>
  );
}
