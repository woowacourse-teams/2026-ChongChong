import { Suspense } from 'react';
import { useSubmitSearch } from '../hooks/useSubmitSearch';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Spinner from '../../../shared/ui/Spinner';
import ErrorContent from '../../../shared/ui/ErrorContent';
import { useSuspenseInfiniteQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import studyQueries from '../queries';
import useInfiniteScroll from '../../../shared/hooks/useInfiniteScroll';
import EmptyContent from '../../../shared/ui/EmptyContent';
import List from '../../../shared/ui/List';
import Badge from '../../../shared/ui/Badge';
import { Search, SearchEmptyContent } from './Search';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { typography } from '../../../styles/global';
import StudyContentCard from './StudyContentCard';

export default function PublicStudies() {
  const { submittedValue, searchValue, handleSearchValue, handleSubmit } = useSubmitSearch();

  return (
    <section aria-label="스터디 둘러보기">
      <Search
        onSubmit={(event) => handleSubmit(event)}
        searchValue={searchValue}
        changeSearchValue={handleSearchValue}
      />
      <ErrorBoundary
        key={submittedValue}
        fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
      >
        <Suspense fallback={<Spinner />}>
          <StudySearchResults keyword={submittedValue} />
        </Suspense>
      </ErrorBoundary>
    </section>
  );
}

function StudySearchResults({ keyword }: { keyword: string }) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isFetchNextPageError } =
    useSuspenseInfiniteQuery(studyQueries.publicList(keyword));
  const studies = data.pages.flatMap((page) => page.studies);
  const loadMoreRef = useInfiniteScroll({
    hasNextPage: hasNextPage && !isFetchNextPageError,
    isFetchingNextPage,
    fetchNextPage,
  });

  return (
    <>
      {studies.length === 0 ? (
        keyword ? (
          <SearchEmptyContent />
        ) : (
          <EmptyContent message="아직 공개된 스터디가 없어요" />
        )
      ) : (
        <>
          <List>
            {studies.map((study) => (
              <List.Item key={study.studyId}>
                <Link {...URL_POLICY.toPublicStudyJoinInfo(study.studyId)}>
                  <StudyContentCard>
                    <StudyContentCard.Badges>
                      <Badge
                        variant="brandSolid"
                        size="small"
                        css={{ ...typography.label, padding: '5px 10px' }}
                      >
                        모집 중
                      </Badge>
                    </StudyContentCard.Badges>
                    <StudyContentCard.Content
                      imageUrl={study.imageUrl}
                      name={study.studyName}
                      description={study.description}
                    />
                  </StudyContentCard>
                </Link>
              </List.Item>
            ))}
          </List>
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
          {isFetchingNextPage && <Spinner size={24} label="스터디를 더 불러오는 중" />}
        </>
      )}
    </>
  );
}
