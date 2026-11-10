import type { CSSProperties, ReactNode } from 'react';
import { tokens, typography } from '../../../styles/global';
import ContentCard from '../../../shared/ui/card/ContentCard';
import rightArrowIcon from '../../../shared/assets/right-arrow.svg';
import StudyAvatar from './StudyAvatar';

interface ContentProps {
  imageUrl?: string | null;
  name: string;
  description: string | null;
}

const StudyCardStyle = {
  display: 'flex',
  width: '100%',
  height: '148px',
  flexDirection: 'column',
  padding: '15px 19px',
  background: tokens.bg.default,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.lg,
  color: tokens.text.default,
} satisfies CSSProperties;

const StudyBadgesStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: '11px',
  marginBottom: tokens.spacing[3],
} satisfies CSSProperties;

const StudyDescriptionStyle = {
  ...typography.body,
  display: '-webkit-box',
  margin: `${tokens.spacing[1]} 0 0`,
  color: tokens.text.muted,
  WebkitBoxOrient: 'vertical',
  WebkitLineClamp: 2,
  whiteSpace: 'normal',
  overflowWrap: 'anywhere',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
} satisfies CSSProperties;

export default function StudyContentCard({ children }: { children: ReactNode }) {
  return <article css={StudyCardStyle}>{children}</article>;
}

StudyContentCard.Badges = function Badges({ children }: { children: ReactNode }) {
  return <div css={StudyBadgesStyle}>{children}</div>;
};

StudyContentCard.Content = function Content({ imageUrl, name, description }: ContentProps) {
  return (
    <div css={{ display: 'flex', alignItems: 'flex-start', gap: tokens.spacing[4] }}>
      <StudyAvatar imageUrl={imageUrl} size={64} variant="list" />
      <div css={{ flex: 1, minWidth: 0 }}>
        <ContentCard.TitleRow>
          <ContentCard.Title size="large">{name}</ContentCard.Title>
          <ContentCard.Trailing>
            <img src={rightArrowIcon} alt="" width={20} height={20} />
          </ContentCard.Trailing>
        </ContentCard.TitleRow>
        <StudyContentCard.Description>{description}</StudyContentCard.Description>
      </div>
    </div>
  );
};

StudyContentCard.Description = function Description({ children }: { children: ReactNode }) {
  return <p css={StudyDescriptionStyle}>{children}</p>;
};
