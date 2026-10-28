import { CSSProperties, ReactNode } from 'react';
import type { CSSObject } from '@emotion/react';
import { Link } from 'react-router';
import { tokens, typography } from '../../../styles/global';
import LaptopLogo from '../../../shared/assets/icons/laptop-icon.svg?width=360&as=webp';
import CheckLogo from '../../../shared/assets/icons/check-icon.svg?width=360&as=webp';
import InviteLogo from '../../../shared/assets/icons/invite-icon.svg';
import { usePostHog } from '@posthog/react';

const BANNER_THEMES = {
  brand: {
    background: tokens.bg.brand,
    text: tokens.text.onBrand,
  },
  mint: {
    background: '#EEFAF5',
    text: tokens.text.primary,
  },
};

type BannerTheme = keyof typeof BANNER_THEMES;

const BannerStyle = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: tokens.spacing[3],
  padding: tokens.spacing[5],
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
  color: 'inherit',
  fontWeight: tokens.fontWeight.semibold,
} satisfies CSSProperties;

const BannerSubTextStyle = {
  ...typography.body,
  margin: 0,
  color: 'inherit',
} satisfies CSSProperties;

const InviteLinkStyle = {
  ...typography.bodyStrong,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  alignSelf: 'flex-start',
  gap: tokens.spacing[2],
  marginTop: tokens.spacing[3],
  color: tokens.text.brand,
} satisfies CSSObject;

interface BannerProps {
  title: string;
  content: ReactNode;
  image: string;
  theme?: BannerTheme;
}

function SubText({ text }: { text: string }) {
  return <p css={BannerSubTextStyle}>{text}</p>;
}

export function Banner({ title, content, image, theme = 'brand' }: BannerProps) {
  const colors = BANNER_THEMES[theme];

  return (
    <div
      css={[
        BannerStyle,
        {
          background: colors.background,
          color: colors.text,
        },
      ]}
    >
      <div css={BannerTextStyle}>
        <p css={BannerTitleStyle}>{title}</p>
        {content}
      </div>
      <img src={image} alt="" css={{ width: '120px', height: '120px' }} />
    </div>
  );
}

export function CheeringBanner({ username }: { username: string }) {
  return (
    <Banner
      title={`${username}님, 오늘도 화이팅!`}
      content={<SubText text="리마인드는 총총이 대신 보낼게요" />}
      image={LaptopLogo}
    />
  );
}

export function TodoBanner({ username, todoCount }: { username: string; todoCount: number }) {
  return (
    <Banner
      title={`${username}님, 할 일이 ${todoCount}건 있어요`}
      content={<SubText text="리마인드는 총총이 대신 보낼게요" />}
      image={CheckLogo}
    />
  );
}

export function InviteMemberBanner({
  studyId,
  invitePageLink,
}: {
  studyId: number;
  invitePageLink: string;
}) {
  const posthog = usePostHog();

  const handleInviteClick = () => {
    posthog?.capture('invite_banner_clicked', {
      study_id: studyId,
      location: 'study_detail_page',
    });
  };

  return (
    <Banner
      title={'함께할 스터디원을 초대해 보세요'}
      content={
        <Link to={invitePageLink} css={InviteLinkStyle} onClick={handleInviteClick}>
          <span>초대하러 가기</span>
          <span aria-hidden="true">→</span>
        </Link>
      }
      image={InviteLogo}
      theme="mint"
    />
  );
}
