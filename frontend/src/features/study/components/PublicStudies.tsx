import { Suspense, useState } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import Input from '../../../shared/ui/inputs/Input';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';
import searchIcon from '../assets/search.svg';
import { STUDY_NAME } from '../constants';
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
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { tokens, typography } from '../../../styles/global';
import rightArrowIcon from '../../../shared/assets/right-arrow.svg';
import StudyContentCard from './StudyContentCard';
import StudyAvatar from './StudyAvatar';

export default function PublicStudies() {
  const [searchValue, setSearchValue] = useState('');
  const [keyword, setKeyword] = useState('');
  return (
    <section aria-label="스터디 둘러보기">
      <form
        role="search"
        aria-label="공개 스터디 검색"
        onSubmit={(event) => {
          event.preventDefault();
          setKeyword(searchValue.trim());
        }}
        css={{ display: 'flex', gap: tokens.spacing[2], marginBottom: tokens.spacing[4] }}
      >
        <Input
          type="search"
          aria-label="스터디 이름 검색"
          placeholder="스터디 이름 검색"
          value={searchValue}
          onChange={(event) => {
            setSearchValue(event.target.value);
            if (!event.target.value) setKeyword('');
          }}
          maxLength={STUDY_NAME.length}
          css={{
            ...typography.body,
            flex: 1,
            minWidth: 0,
            minHeight: 40,
            height: 40,
            borderRadius: 20,
            padding: `0 ${tokens.spacing[5]}`,
          }}
        />
        <Button
          type="submit"
          variant="brandSolid"
          size="small"
          aria-label="검색"
          css={{ width: 40, height: 40, flexShrink: 0, padding: 0, borderRadius: 20 }}
        >
          <img src={searchIcon} alt="" width={20} height={20} />
        </Button>
      </form>
      <ErrorBoundary
        key={keyword}
        fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
      >
        <Suspense fallback={<Loading />}>
          <PublicStudyResults keyword={keyword} />
        </Suspense>
      </ErrorBoundary>
    </section>
  );
}

function PublicStudyResults({ keyword }: { keyword: string }) {
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
        <EmptyContent message={keyword ? '검색 결과가 없어요' : '아직 공개된 스터디가 없어요'} />
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
