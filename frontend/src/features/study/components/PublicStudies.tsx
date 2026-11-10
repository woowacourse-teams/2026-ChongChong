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
import { Paragraph } from '../../../shared/ui/Typography';
import Button from '../../../shared/ui/Button';
import List from '../../../shared/ui/List';
import Badge from '../../../shared/ui/Badge';
import ContentCard from '../../../shared/ui/card/ContentCard';
import { Search, SearchEmptyContent } from './Search';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { tokens, typography } from '../../../styles/global';
import rightArrowIcon from '../../../shared/assets/right-arrow.svg';
import StudyContentCard from './StudyContentCard';
import StudyAvatar from './StudyAvatar';

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
          <PublicStudyResults value={submittedValue} />
        </Suspense>
      </ErrorBoundary>
    </section>
  );
}

function PublicStudyResults({ value }: { value: string }) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isFetchNextPageError } =
    useSuspenseInfiniteQuery(studyQueries.publicList(value));
  const studies = data.pages.flatMap((page) => page.studies);
  const loadMoreRef = useInfiniteScroll({
    hasNextPage: hasNextPage && !isFetchNextPageError,
    isFetchingNextPage,
    fetchNextPage,
  });

  return (
    <>
      {studies.length === 0 ? (
        value ? (
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
                    <div
                      css={{ display: 'flex', alignItems: 'flex-start', gap: tokens.spacing[4] }}
                    >
                      <StudyAvatar imageUrl={study.imageUrl} size={64} variant="list" />
                      <div css={{ flex: 1, minWidth: 0 }}>
                        <ContentCard.TitleRow>
                          <ContentCard.Title size="large">{study.studyName}</ContentCard.Title>
                          <ContentCard.Trailing>
                            <img src={rightArrowIcon} alt="" width={20} height={20} />
                          </ContentCard.Trailing>
                        </ContentCard.TitleRow>
                        <StudyContentCard.Description>
                          {study.description}
                        </StudyContentCard.Description>
                      </div>
                    </div>
                  </StudyContentCard>
                </Link>
              </List.Item>
            ))}
          </List>
          <div ref={loadMoreRef} css={{ minHeight: '1px' }} aria-hidden="true" />
          {isFetchingNextPage && (
            <Paragraph variant="inherited" role="status">
              스터디를 더 불러오는 중...
            </Paragraph>
          )}
          {isFetchNextPageError && (
            <div>
              <Paragraph variant="inherited" role="alert">
                스터디를 더 불러오지 못했어요.
              </Paragraph>
              <Button
                variant="brandOutline"
                size="small"
                onClick={() => void fetchNextPage()}
                disabled={isFetchingNextPage}
              >
                다시 시도
              </Button>
            </div>
          )}
        </>
      )}
    </>
  );
}
