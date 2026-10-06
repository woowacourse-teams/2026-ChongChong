import { tokens } from '../../../styles/global';
import bowingChongchong from '../../../shared/assets/icons/chongchong-greeting.png';

export default function CompleteArtwork() {
  return (
    <div
      aria-hidden="true"
      css={{
        position: 'relative',
        width: '220px',
        maxWidth: '100%',
        height: '138px',
        alignSelf: 'center',
        flexShrink: 0,
        '@media (max-height: 650px)': { width: '176px', height: '110px' },
      }}
    >
      <svg
        viewBox="0 0 220 138"
        fill="none"
        css={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}
      >
        <ellipse cx="110" cy="127" rx="54" ry="9" fill={tokens.bg.brandSubtle} />
        <path
          d="M39 38v8m-4-4h8"
          stroke={tokens.bg.brand}
          strokeWidth="2.5"
          strokeLinecap="round"
        />
        <path d="M179 51v8m-4-4h8" stroke="#f4c96b" strokeWidth="2.5" strokeLinecap="round" />
      </svg>
      <img
        src={bowingChongchong}
        alt=""
        width={180}
        height={180}
        css={{
          position: 'absolute',
          left: '50%',
          top: '50%',
          transform: 'translate(-50%, -50%)',
          width: '180px',
          maxWidth: '100%',
          height: '180px',
          objectFit: 'contain',
          '@media (max-height: 650px)': { width: '144px', height: '144px' },
        }}
      />
    </div>
  );
}
