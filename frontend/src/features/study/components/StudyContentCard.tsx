import type { ComponentPropsWithoutRef, CSSProperties, ReactNode } from 'react';
import { useId } from 'react';
import { tokens } from '../../../styles/global';
import ContentCard from '../../../shared/ui/card/ContentCard';
import rightArrowIcon from '../../../shared/assets/right-arrow.svg';
import StudyAvatar from './StudyAvatar';
import memberIcon from '../../../shared/assets/member-count.svg';
import { Text } from '../../../shared/ui/Typography';

interface StudyCardData {
  imageUrl?: string | null;
  name: string;
  description: string | null;
  memberCount: number;
}

type StudyCardLayout = 'horizontal' | 'vertical';

const StudyCardStyle = {
  display: 'flex',
  flexDirection: 'column',
  background: tokens.bg.default,
  border: tokens.border.neutral,
  borderRadius: tokens.radius.lg,
  color: tokens.text.default,
} satisfies CSSProperties;

const LayoutStyles = {
  horizontal: {
    width: '100%',
    height: '148px',
    padding: '15px 19px',
  },
  vertical: {
    alignItems: 'center',
    minHeight: 168,
    padding: `19px ${tokens.spacing[5]} ${tokens.spacing[4]}`,
    textAlign: 'center',
  },
} satisfies Record<StudyCardLayout, CSSProperties>;

const StudyBadgesStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: '11px',
  marginBottom: tokens.spacing[3],
} satisfies CSSProperties;

const memberCountStyle = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: tokens.spacing[1],
  color: tokens.text.brand,
  flexShrink: 0,
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

interface HorizontalStudyCardProps {
  study: StudyCardData;
  badges: ReactNode;
}

export function HorizontalStudyCard({ study, badges }: HorizontalStudyCardProps) {
  return (
    <StudyContentCard layout="horizontal">
      <StudyContentCard.Badges>{badges}</StudyContentCard.Badges>
      <div css={{ display: 'flex', alignItems: 'flex-start', gap: tokens.spacing[4] }}>
        <StudyAvatar imageUrl={study.imageUrl} size={64} variant="list" />
        <div css={{ flex: 1, minWidth: 0 }}>
          <ContentCard.TitleRow css={{ justifyContent: 'space-between' }}>
            <ContentCard.Title size="large">{study.name}</ContentCard.Title>
            <div
              css={{
                display: 'flex',
                alignItems: 'center',
                gap: tokens.spacing[3],
                flexShrink: 0,
              }}
            >
              <StudyContentCard.MemberCount count={study.memberCount} />
              <ContentCard.Trailing>
                <img src={rightArrowIcon} alt="" width={20} height={20} />
              </ContentCard.Trailing>
            </div>
          </ContentCard.TitleRow>
          <ContentCard.Description css={{ whiteSpace: 'normal', overflowWrap: 'anywhere' }}>
            {study.description}
          </ContentCard.Description>
        </div>
      </div>
    </StudyContentCard>
  );
}

interface VerticalStudyCardProps {
  study: Pick<StudyCardData, 'imageUrl' | 'name' | 'description'>;
}

export function VerticalStudyCard({ study }: VerticalStudyCardProps) {
  const titleId = useId();

  return (
    <StudyContentCard layout="vertical" aria-labelledby={titleId}>
      <StudyAvatar
        imageUrl={study.imageUrl}
        size={68}
        variant="management"
        alt={`${study.name} 프로필`}
      />
      <ContentCard.Title
        id={titleId}
        level={2}
        size="large"
        truncate={false}
        css={{ marginTop: tokens.spacing[4] }}
      >
        {study.name}
      </ContentCard.Title>
      {study.description && (
        <ContentCard.Description truncate={false}>{study.description}</ContentCard.Description>
      )}
    </StudyContentCard>
  );
}

interface StudyContentCardProps extends ComponentPropsWithoutRef<'article'> {
  layout?: StudyCardLayout;
}

export function StudyContentCard({
  children,
  layout = 'horizontal',
  ...props
}: StudyContentCardProps) {
  const Tag = layout === 'vertical' ? 'section' : 'article';

  return (
    <Tag css={[StudyCardStyle, LayoutStyles[layout]]} {...props}>
      {children}
    </Tag>
  );
}

StudyContentCard.Badges = function Badges({ children }: { children: ReactNode }) {
  return <div css={StudyBadgesStyle}>{children}</div>;
};

StudyContentCard.MemberCount = function MemberCount({ count }: { count: number }) {
  return (
    <Text variant="footnote" css={memberCountStyle}>
      <img src={memberIcon} alt="" width={12} height={12} css={{ flexShrink: 0 }} />
      {`${count}명`}
    </Text>
  );
};
