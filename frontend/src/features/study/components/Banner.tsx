import { CSSProperties, ReactNode } from 'react';
import type { CSSObject } from '@emotion/react';
import { Link } from 'react-router';
import { tokens, typography } from '../../../styles/global';
import LaptopLogo from '../../../shared/assets/icons/laptop-icon.svg';
import CheckLogo from '../../../shared/assets/icons/check-icon.svg';
import InviteLogo from '../../../shared/assets/icons/invite-icon.webp';

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

const InviteLinkStyle = {
  ...typography.bodyStrong,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  alignSelf: 'flex-start',
  gap: tokens.spacing[2],
  marginTop: tokens.spacing[3],
  padding: `${tokens.spacing[2]} ${tokens.spacing[3]}`,
  borderRadius: tokens.radius.md,
  background: tokens.bg.default,
  color: tokens.text.default,
  '&:hover': {
    background: tokens.bg.brandSubtle,
  },
} satisfies CSSObject;

interface BannerProps {
  title: string;
  description: ReactNode;
  image: string;
}

function SubText({ text }: { text: string }) {
  return <p css={BannerDescriptionStyle}>{text}</p>;
}

export function Banner({ title, description, image }: BannerProps) {
  return (
    <div css={BannerStyle}>
      <div css={BannerTextStyle}>
        <p css={BannerTitleStyle}>{title}</p>
        {description}
      </div>
      <img src={image} alt="" css={{ width: '120px', height: '120px' }} />
    </div>
  );
}

export function CheeringBanner({ username }: { username: string }) {
  return (
    <Banner
      title={`${username}님, 오늘도 화이팅!`}
      description={<SubText text="리마인드는 총총이 대신 보낼게요" />}
      image={LaptopLogo}
    />
  );
}

export function TodoBanner({ username, todoCount }: { username: string; todoCount: number }) {
  return (
    <Banner
      title={`${username}님, 할 일이 ${todoCount}건 있어요`}
      description={<SubText text="리마인드는 총총이 대신 보낼게요" />}
      image={CheckLogo}
    />
  );
}

export function InviteMemberBanner({ invitePageLink }: { invitePageLink: string }) {
  return (
    <Banner
      title={'함께할 스터디원을 초대해 보세요'}
      description={
        <Link to={invitePageLink} css={InviteLinkStyle}>
          <span>초대하러 가기</span>
          <span aria-hidden="true">→</span>
        </Link>
      }
      image={InviteLogo}
    />
  );
}
