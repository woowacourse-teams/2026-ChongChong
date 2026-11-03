import { Heading } from '../../../shared/ui/Typography';
import { useSuspenseQuery } from '@tanstack/react-query';
import studyQueries from '../queries';
import StudyList from './StudyList';
import EmptyContent from '../../../shared/ui/EmptyContent';
import { CSSProperties } from 'react';

const sectionStyle = {
  display: 'flex',
  flex: 1,
  flexDirection: 'column',
} satisfies CSSProperties;

export default function MyStudies() {
  const { data: studies } = useSuspenseQuery({
    ...studyQueries.list(),
    select: (data) => data.studies,
  });

  return (
    <section css={sectionStyle} aria-labelledby="my-studies-heading">
      <Heading level={2} variant="subtitle" id="my-studies-heading">
        내 스터디
      </Heading>
      {studies.length === 0 ? (
        <EmptyContent message="아직 스터디가 없어요" />
      ) : (
        <StudyList studies={studies} />
      )}
    </section>
  );
}
