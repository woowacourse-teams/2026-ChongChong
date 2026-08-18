import List from '../../../shared/ui/List';
import ContentCard from '../../../shared/ui/card/ContentCard';
import Badge from '../../../shared/ui/Badge';
import noticeIcon from '../../../shared/assets/notice.svg';
import assignIcon from '../../../shared/assets/assign.svg';
import rightArrowIcon from '../../../shared/assets/right-arrow.svg';
import { Study } from '../types';

export default function StudyList({ studies }: { studies: Study[] }) {
  return (
    <List>
      {studies.map((study) => {
        return (
          <List.Item key={study.id}>
            <ContentCard>
              <ContentCard.Badges>
                <Badge variant="BrandSolid" size="Small">
                  {study.role === 'STUDY_LEADER' ? '스터디 리드' : '스터디원'}
                </Badge>
              </ContentCard.Badges>

              <ContentCard.TitleRow>
                <ContentCard.Title>{study.title}</ContentCard.Title>
                <ContentCard.Trailing>
                  <a href="">
                    <img src={rightArrowIcon} alt="" css={{ width: '20px', height: '20px' }} />
                  </a>
                </ContentCard.Trailing>
              </ContentCard.TitleRow>

              <ContentCard.Description>{study.description}</ContentCard.Description>

              <ContentCard.Footer>
                <ContentCard.Badge variant="NeutralSolid" size="Small">
                  <img src={noticeIcon} alt="" css={{ width: '12px', height: '12px' }} />
                  공지 {study.noticeCount}
                </ContentCard.Badge>
                <ContentCard.Badge variant="NeutralSolid" size="Small">
                  <img src={assignIcon} alt="" css={{ width: '12px', height: '12px' }} />
                  과제 {study.assignmentCount}
                </ContentCard.Badge>
              </ContentCard.Footer>
            </ContentCard>
          </List.Item>
        );
      })}
    </List>
  );
}
