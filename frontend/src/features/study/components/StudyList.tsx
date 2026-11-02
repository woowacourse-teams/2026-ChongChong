import StudyAvatar from './StudyAvatar';
import type { CSSProperties } from 'react';
import StudyContentCard from './StudyContentCard';
import List from '../../../shared/ui/List';
import ContentCard from '../../../shared/ui/card/ContentCard';
import Badge from '../../../shared/ui/Badge';
import noticeIcon from '../../../shared/assets/notice.webp';
import assignIcon from '../../../shared/assets/assign.webp';
import rightArrowIcon from '../../../shared/assets/right-arrow.svg';
import { tokens, typography } from '../../../styles/global';
import { URL_POLICY } from '../../../shared/constants/urlPolicy';
import { Link } from 'react-router';
import { Study } from '../types';

const RoleBadgeStyle = {
  ...typography.label,
  padding: '5px 10px',
  marginRight: '3px',
  flexShrink: 0,
} satisfies CSSProperties;

const CountBadgeStyle = {
  ...typography.label,
  minWidth: '64px',
  height: '24px',
  flexShrink: 0,
  whiteSpace: 'nowrap',
} satisfies CSSProperties;

export default function StudyList({ studies }: { studies: Study[] }) {
  return (
    <List>
      {studies.map((study) => (
        <List.Item key={study.id}>
          <Link {...URL_POLICY.toStudy(study.id)}>
            <StudyContentCard>
              <StudyContentCard.Badges>
                <Badge variant="brandSolid" size="small" css={RoleBadgeStyle}>
                  {study.role === 'LEADER' ? '스터디 리드' : '스터디원'}
                </Badge>
                <Badge variant="neutralSolid" size="small" css={CountBadgeStyle}>
                  <img src={noticeIcon} alt="" width={12} height={12} />
                  공지 {study.noticeCount}
                </Badge>
                <Badge variant="neutralSolid" size="small" css={CountBadgeStyle}>
                  <img src={assignIcon} alt="" width={12} height={12} />
                  과제 {study.assignmentCount}
                </Badge>
              </StudyContentCard.Badges>
              <div css={{ display: 'flex', alignItems: 'flex-start', gap: tokens.spacing[4] }}>
                <StudyAvatar imageUrl={study.imageUrl} size={64} variant="list" />
                <div css={{ flex: 1, minWidth: 0 }}>
                  <ContentCard.TitleRow>
                    <ContentCard.Title size="large">{study.name}</ContentCard.Title>
                    <ContentCard.Trailing>
                      <img src={rightArrowIcon} alt="" width={20} height={20} />
                    </ContentCard.Trailing>
                  </ContentCard.TitleRow>
                  <StudyContentCard.Description>{study.description}</StudyContentCard.Description>
                </div>
              </div>
            </StudyContentCard>
          </Link>
        </List.Item>
      ))}
    </List>
  );
}
