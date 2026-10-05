import { tokens, typography } from '../../../styles/global';

interface Props {
  onClick: () => void;
}

export default function GuidelineDismissButton({ onClick }: Props) {
  return (
    <button
      type="button"
      onClick={onClick}
      css={{
        ...typography.caption,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: 'fit-content',
        maxWidth: '100%',
        minHeight: '44px',
        flexShrink: 0,
        whiteSpace: 'nowrap',
        padding: '6px 0',
        border: 0,
        borderRadius: tokens.radius.full,
        background: 'transparent',
        color: '#007b48',
        fontWeight: tokens.fontWeight.medium,
        cursor: 'pointer',
        '@media (hover: hover)': {
          '&:hover > span': {
            background: 'rgba(0, 196, 113, 0.14)',
            borderColor: 'rgba(0, 123, 72, 0.24)',
          },
        },
        '&:focus-visible': {
          outline: '2px solid #007b48',
          outlineOffset: '3px',
        },
        '&:active > span': {
          background: 'rgba(0, 196, 113, 0.18)',
          transform: 'translateY(1px)',
        },
        '@media (prefers-reduced-motion: reduce)': {
          '& > span': { transition: 'none' },
          '&:active > span': { transform: 'none' },
        },
      }}
    >
      <span
        css={{
          boxSizing: 'border-box',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          gap: tokens.spacing[1],
          height: '32px',
          padding: '0 10px',
          border: '1px solid rgba(0, 123, 72, 0.12)',
          borderRadius: tokens.radius.full,
          background: tokens.bg.brandSubtle,
          transition: 'background-color 150ms ease, border-color 150ms ease, transform 150ms ease',
        }}
      >
        <svg
          width="14"
          height="14"
          viewBox="0 0 18 18"
          fill="none"
          aria-hidden="true"
          focusable="false"
          css={{ flexShrink: 0 }}
        >
          <path
            d="M15 4.5L6.75 12.75L3 9"
            stroke="currentColor"
            strokeWidth="1.75"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
        다시 보지 않기
      </span>
    </button>
  );
}
