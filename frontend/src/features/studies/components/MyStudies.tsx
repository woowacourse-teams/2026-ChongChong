import { useSuspenseQuery } from '@tanstack/react-query';
import studyQueries from '../queries';
import StudyList from './StudyList';
import EmptyState from '../../../shared/ui/EmptyState';
import { typography } from '../../../styles/global';

export default function MyStudies() {
  const { data: studies } = useSuspenseQuery({
    ...studyQueries.list(),
    select: (data) => data.studies,
  });

  return (
    <section aria-labelledby="my-studies-heading">
      <h2 id="my-studies-heading" css={typography.subtitle}>
        내 스터디
      </h2>
      {studies.length === 0 ? (
        <EmptyState message="아직 스터디가 없어요" />
      ) : (
        <StudyList studies={studies} />
      )}
    </section>
  );
}
