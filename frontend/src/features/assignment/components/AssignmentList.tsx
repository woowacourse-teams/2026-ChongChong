import rightArrow from '../../../shared/assets/right-arrow.svg';
import ContentCard from '../../../shared/ui/card/ContentCard';
import List from '../../../shared/ui/List';
import { Link } from 'react-router';
import { Assignment } from '../types';
import { ReactNode } from 'react';
import { formatDeadline } from '../../../shared/utils/formatDate';
import LinkIcon from '../../../shared/assets/link.svg';

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
              <ContentCard.Badges>{children(assignment)}</ContentCard.Badges>
              <ContentCard.TitleRow>
                <ContentCard.Title>{assignment.title}</ContentCard.Title>

                <ContentCard.Trailing>
                  <img src={rightArrow} alt="" width={20} height={20} />
                </ContentCard.Trailing>
              </ContentCard.TitleRow>

              <ContentCard.Description>{assignment.content}</ContentCard.Description>

              <ContentCard.Footer direction="column">
                <ContentCard.Meta tone="brand">
                  {formatDeadline(assignment.closeAt) + ' 마감'}
                </ContentCard.Meta>
                <ContentCard.Meta>
                  <img src={LinkIcon} alt="" width={13} height={13} />
                  {assignment.submissionMethod}
                </ContentCard.Meta>
              </ContentCard.Footer>
            </ContentCard>
          </Link>
        </List.Item>
      ))}
    </List>
  );
}
