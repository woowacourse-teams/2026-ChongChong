import List from '../../../shared/ui/List';
import ContentCard from '../../../shared/ui/card/ContentCard';
import Badge from '../../../shared/ui/Badge';
import noticeIcon from '../../../shared/assets/notice.svg';
import assignIcon from '../../../shared/assets/assign.svg';
import rightArrowIcon from '../../../shared/assets/right-arrow.svg';
import { Link } from 'react-router';
import { Study } from '../types';

export default function StudyList({ studies }: { studies: Study[] }) {
  return (
    <List>
      {studies.map((study) => {
        return (
          <List.Item key={study.id}>
            <Link to={`/studies/${study.id}`}>
              <ContentCard>
                <ContentCard.Badges>
                  <Badge variant="brandSolid" size="small">
                    {study.role === 'LEADER' ? '스터디 리드' : '스터디원'}
                  </Badge>
                </ContentCard.Badges>

                <ContentCard.TitleRow>
                  <ContentCard.Title>{study.name}</ContentCard.Title>
                  <ContentCard.Trailing>
                    <img src={rightArrowIcon} alt="" css={{ width: '20px', height: '20px' }} />
                  </ContentCard.Trailing>
                </ContentCard.TitleRow>

                <ContentCard.Description>{study.description}</ContentCard.Description>

                <ContentCard.Footer>
                  <ContentCard.Badge variant="neutralSolid" size="small">
                    <img src={noticeIcon} alt="" css={{ width: '12px', height: '12px' }} />
                    공지 {study.noticeCount}
                  </ContentCard.Badge>
                  <ContentCard.Badge variant="neutralSolid" size="small">
                    <img src={assignIcon} alt="" css={{ width: '12px', height: '12px' }} />
                    과제 {study.assignmentCount}
                  </ContentCard.Badge>
                </ContentCard.Footer>
              </ContentCard>
            </Link>
          </List.Item>
        );
      })}
    </List>
  );
}
