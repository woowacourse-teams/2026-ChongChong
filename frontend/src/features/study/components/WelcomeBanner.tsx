import { CSSProperties } from 'react';
import { tokens, typography } from '../../../styles/global';
import LaptopLogo from '../../../shared/assets/icons/laptop-icon.svg';
import CheckLogo from '../../../shared/assets/icons/check-icon.svg';

const BannerStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: tokens.spacing[3],
  padding: tokens.spacing[5],
  background: tokens.bg.brand,
  borderRadius: tokens.radius.lg,
  marginBottom: tokens.spacing[8],
} satisfies CSSProperties;

const BannerTextStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: tokens.spacing[2],
  minWidth: 0,
} satisfies CSSProperties;

const BannerTitleStyle = {
  ...typography.title,
  margin: 0,
  color: tokens.text.onBrand,
  fontWeight: tokens.fontWeight.semibold,
} satisfies CSSProperties;

const BannerDescriptionStyle = {
  ...typography.body,
  margin: 0,
  color: tokens.text.onBrand,
} satisfies CSSProperties;

export function StudyLeaderWelcomeBanner({ username }: { username: string }) {
  return (
    <div css={BannerStyle}>
      <div css={BannerTextStyle}>
        <p css={BannerTitleStyle}>{username}님, 오늘도 화이팅!</p>
        <p css={BannerDescriptionStyle}>리마인드는 총총이 대신 보낼게요</p>
      </div>
      <img src={LaptopLogo} alt="" css={{ width: '120px', height: '120px' }} />
    </div>
  );
}

export function StudyMemberWelcomeBanner({
  username,
  todoCount,
}: {
  username: string;
  todoCount: number;
}) {
  return (
    <div css={BannerStyle}>
      <div css={BannerTextStyle}>
        <p css={BannerTitleStyle}>
          {username}님, 할 일이 {todoCount}건 있어요
        </p>
        <p css={BannerDescriptionStyle}>리마인드는 총총이 대신 보낼게요</p>
      </div>
      <img src={CheckLogo} alt="" css={{ width: '120px', height: '120px' }} />
    </div>
  );
}
