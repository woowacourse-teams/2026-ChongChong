import { useSuspenseQuery } from '@tanstack/react-query';
import studyQueries from '../queries';
import List from '../../../shared/ui/List';
import { Link } from 'react-router';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import StudyContentCard from './StudyContentCard';
import Badge from '../../../shared/ui/Badge';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { CSSProperties } from 'react';
import { typography } from '../../../styles/global';
import noticeIcon from '../../../shared/assets/notice.webp';
import assignIcon from '../../../shared/assets/assign.webp';

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

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

export default function MyStudies() {
  const { data: studies } = useSuspenseQuery({
    ...studyQueries.list(),
    select: (data) => data.studies,
  });

  return (
    <section css={sectionStyle} aria-label="내 스터디">
      {studies.length === 0 ? (
        <EmptyContent message="아직 스터디가 없어요" />
      ) : (
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
                  <StudyContentCard.Content
                    imageUrl={study.imageUrl}
                    name={study.name}
                    description={study.description}
                  />
                </StudyContentCard>
              </Link>
            </List.Item>
          ))}
        </List>
      )}
    </section>
  );
}
