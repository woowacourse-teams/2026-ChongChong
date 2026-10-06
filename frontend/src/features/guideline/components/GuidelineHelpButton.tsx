import { tokens, typography } from '../../../styles/global';

export default function GuidelineHelpButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      type="button"
      aria-haspopup="dialog"
      onClick={(event) => {
        event.currentTarget.focus();
        onClick();
      }}
      css={{
        ...typography.caption,
        minHeight: '44px',
        padding: 0,
        border: 0,
        borderRadius: tokens.radius.sm,
        background: 'transparent',
        color: tokens.text.secondary,
        cursor: 'pointer',
        textUnderlineOffset: '3px',
        '&:hover': { color: tokens.text.brand, textDecoration: 'underline' },
        '&:focus-visible': { outline: `2px solid ${tokens.text.default}`, outlineOffset: '3px' },
      }}
    >
      총총 어떻게 사용해요?
    </button>
  );
}
