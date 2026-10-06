import { keyframes } from '@emotion/react';
import chongchong from '../../landing/assets/chongchong-logo.png';
import notificationIcon from '../../../shared/assets/notification.svg';
import linkIcon from '../../../shared/assets/link-green.svg';
import { tokens } from '../../../styles/global';
import CompleteArtwork from './CompleteArtwork';

type Theme = 'welcome' | 'pwa' | 'notification' | 'invitation' | 'complete';

const float = keyframes`
  0%, 100% { transform: translateY(0) rotate(-4deg); }
  50% { transform: translateY(-5px) rotate(0deg); }
`;

const appear = keyframes`
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
`;

const cardStyle = {
  position: 'absolute' as const,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: tokens.bg.default,
  border: tokens.border.default,
  boxShadow: tokens.shadow[2],
};

export default function GuidelineArtwork({ theme }: { theme: Theme }) {
  if (theme === 'complete') return <CompleteArtwork />;

  const hasCard = theme === 'notification' || theme === 'invitation';

  return (
    <div
      aria-hidden="true"
      css={{
        position: 'relative',
        width: '200px',
        maxWidth: '100%',
        height: '126px',
        alignSelf: 'center',
        flexShrink: 0,
      }}
    >
      <div
        css={{
          position: 'absolute',
          inset: '15px 29px 0',
          borderRadius: '50%',
          background: tokens.bg.brandSubtle,
        }}
      />
      <span
        css={{
          position: 'absolute',
          width: '8px',
          height: '8px',
          borderRadius: tokens.radius.full,
          background: tokens.bg.brand,
          opacity: 0.5,
          left: '25px',
          top: '24px',
        }}
      />
      <span
        css={{
          position: 'absolute',
          width: '5px',
          height: '5px',
          borderRadius: tokens.radius.full,
          background: tokens.bg.brand,
          right: '18px',
          bottom: '24px',
        }}
      />
      {theme === 'pwa' ? (
        <div
          css={{
            ...cardStyle,
            top: '2px',
            left: '63px',
            width: '74px',
            height: '120px',
            border: `2px solid ${tokens.text.primary}`,
            borderRadius: tokens.radius.lg,
            flexDirection: 'column',
            gap: tokens.spacing[2],
            transform: 'rotate(-5deg)',
            '&::before': {
              content: '""',
              position: 'absolute',
              top: '6px',
              height: '3px',
              width: '22px',
              borderRadius: tokens.radius.full,
              background: tokens.bg.chip,
            },
          }}
        >
          <img src={chongchong} alt="" width={43} height={48} />
          <span css={{ width: '28px', height: '4px', background: tokens.bg.brandSubtle }} />
        </div>
      ) : (
        <img
          src={chongchong}
          alt=""
          width={96}
          height={106}
          css={{
            position: 'absolute',
            bottom: hasCard ? '12px' : '4px',
            left: hasCard ? '32px' : '52px',
            objectFit: 'contain',
            animation: `${float} 3.5s ease-in-out infinite`,
            '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
          }}
        />
      )}
      {hasCard && (
        <div
          css={{
            ...cardStyle,
            right: '4px',
            bottom: '14px',
            width: '88px',
            height: '54px',
            gap: tokens.spacing[2],
            padding: tokens.spacing[2],
            borderRadius: tokens.radius.md,
            animation: `${appear} 450ms ease-out both`,
            '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
          }}
        >
          <img
            src={theme === 'notification' ? notificationIcon : linkIcon}
            alt=""
            width={24}
            height={24}
          />
          <div css={{ display: 'grid', gap: '6px' }}>
            <span css={{ width: '24px', height: '4px', background: tokens.bg.chip }} />
            <span css={{ width: '16px', height: '4px', background: tokens.bg.brandSubtle }} />
          </div>
        </div>
      )}
    </div>
  );
}
