import StudyAvatar from './StudyAvatar';
import List from '../../../shared/ui/List';
import ContentCard from '../../../shared/ui/card/ContentCard';
import Badge from '../../../shared/ui/Badge';
import noticeIcon from '../assets/study-notice.svg';
import assignIcon from '../assets/study-assignment.svg';
import rightArrowIcon from '../assets/study-chevron.svg';
import { tokens, typography } from '../../../styles/global';
import { Link } from 'react-router';
import { Study } from '../types';

export default function StudyList({ studies }: { studies: Study[] }) {
  return (
    <List>
      {studies.map((study) => (
        <List.Item key={study.id}>
          <Link to={`/studies/${study.id}`}>
            <ContentCard css={{ height: 'auto', minHeight: 148, padding: '15px 19px 19px' }}>
              <ContentCard.Badges
                css={{ gap: tokens.spacing[3], marginBottom: tokens.spacing[3], flexWrap: 'wrap' }}
              >
                <Badge
                  variant="brandSolid"
                  size="small"
                  css={{ ...typography.label, padding: '5px 10px' }}
                >
                  {study.role === 'LEADER' ? '스터디 리드' : '스터디원'}
                </Badge>
                <Badge variant="neutralSolid" size="small" css={typography.label}>
                  <img src={noticeIcon} alt="" />
                  공지 {study.noticeCount}
                </Badge>
                <Badge variant="neutralSolid" size="small" css={typography.label}>
                  <img src={assignIcon} alt="" />
                  과제 {study.assignmentCount}
                </Badge>
              </ContentCard.Badges>
              <div css={{ display: 'flex', alignItems: 'flex-start', gap: tokens.spacing[4] }}>
                <StudyAvatar imageUrl={study.imageUrl} size={64} variant="list" />
                <div css={{ flex: 1, minWidth: 0 }}>
                  <ContentCard.TitleRow>
                    <ContentCard.Title size="large">{study.name}</ContentCard.Title>
                    <ContentCard.Trailing>
                      <img src={rightArrowIcon} alt="" />
                    </ContentCard.Trailing>
                  </ContentCard.TitleRow>
                  <ContentCard.Description css={{ whiteSpace: 'normal', overflowWrap: 'anywhere' }}>
                    {study.description}
                  </ContentCard.Description>
                </div>
              </div>
            </ContentCard>
          </Link>
        </List.Item>
      ))}
    </List>
  );
}
