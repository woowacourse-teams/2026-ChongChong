// import type { ReactNode } from 'react';
import rightArrow from '../../../shared/assets/right-arrow.svg';
import ContentCard from '../../../shared/ui/card/ContentCard';
import List from '../../../shared/ui/List';
import { Link } from 'react-router';
import { Assignment } from '../types';
import { ReactNode } from 'react';

interface AssigmentListProps {
  assignments: Assignment[];
  studyId: number;
  children: (assignment: Assignment) => ReactNode;
}

export default function AssigmentList({ assignments, studyId, children }: AssigmentListProps) {
  return (
    <List>
      {assignments.map((assignment) => (
        <List.Item key={assignment.id}>
          <Link
            to={{
              pathname: `/studies/${studyId}/assignments/${assignment.id}`,
            }}
          >
            <ContentCard>
              <ContentCard.Badges>
                {/* <Badge variant="BrandOutline" size="Small">
                  {assignment.completeCount}/{assignment.memberCount} 제출
                </Badge> */}
                {children(assignment)}
              </ContentCard.Badges>
              <ContentCard.TitleRow>
                <ContentCard.Title>{assignment.title}</ContentCard.Title>

                <ContentCard.Trailing>
                  <img src={rightArrow} alt="공지 상세 보기" width={20} height={20} />
                </ContentCard.Trailing>
              </ContentCard.TitleRow>

              <ContentCard.Description>{assignment.content}</ContentCard.Description>

              <ContentCard.Footer direction="column">
                <ContentCard.Meta tone="brand">{assignment.closeAt}</ContentCard.Meta>
                <ContentCard.Meta>{assignment.submissionType}</ContentCard.Meta>
              </ContentCard.Footer>
            </ContentCard>
          </Link>
        </List.Item>
      ))}
    </List>
  );
}
