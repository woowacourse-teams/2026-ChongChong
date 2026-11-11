import { Link, useSearchParams } from 'react-router';
import { Suspense, useEffect, type ReactNode } from 'react';
import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import { useSuspenseQuery } from '@tanstack/react-query';
import { usePostHog } from '@posthog/react';
import Button from '../../../shared/ui/Button';
import Loading from '../../../shared/ui/Loading';
import ErrorContent from '../../../shared/ui/ErrorContent';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import { URL_POLICY } from '../../../shared/policy/urlPolicy';
import { tokens } from '../../../styles/global';
import usePageNavigation from '../../../shared/hooks/usePageNavigation';
import NotificationLink from '../../notification/components/NotificationLink';
import MyIcon from '../../../shared/assets/my.webp';
import { VerticalStudyCard } from '../components/StudyContentCard';
import studyQueries from '../queries';
import type { StudyJoinTarget } from '../types';
import useStudyJoin from '../hooks/useStudyJoin';

export default function StudyJoinConfirmPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const studyId = Number(searchParams.get('studyId'));

  return (
    <RequireJoinTarget token={token} studyId={studyId}>
      {(target) => (
        <Page>
          <TopHeader
            left={
              <>
                <PrevButton />
                <TopHeader.Title>스터디 참여하기</TopHeader.Title>
              </>
            }
            right={
              <div css={{ display: 'flex', gap: tokens.spacing[5], alignItems: 'center' }}>
                <NotificationLink />
                <Link {...URL_POLICY.toMyPage()} aria-label="마이페이지">
                  <img width={28} height={28} src={MyIcon} alt="" />
                </Link>
              </div>
            }
          />
          <Main css={{ paddingTop: tokens.spacing[3] }}>
            <ErrorBoundary
              fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
            >
              <Suspense fallback={<Loading />}>
                <Content target={target} />
              </Suspense>
            </ErrorBoundary>
          </Main>
        </Page>
      )}
    </RequireJoinTarget>
  );
}

function RequireJoinTarget({
  token,
  studyId,
  children,
}: {
  token: string | null;
  studyId: number;
  children: (target: StudyJoinTarget) => ReactNode;
}) {
  const { move } = usePageNavigation();
  const target = resolveStudyJoinTarget(token, studyId);
  const hasTarget = target !== null;

  function resolveStudyJoinTarget(token: string | null, studyId: number): StudyJoinTarget | null {
    if (token?.trim()) return { token };
    if (Number.isSafeInteger(studyId) && studyId > 0) return { studyId };
    return null;
  }

  useEffect(() => {
    if (!hasTarget) move(URL_POLICY.afterMissingInviteToken());
  }, [hasTarget, move]);
  return target ? children(target) : null;
}

function Content({ target }: { target: StudyJoinTarget }) {
  const { data } = useSuspenseQuery(studyQueries.joinInfo(target.token ?? target));
  const { mutate: joinStudy, isPending } = useStudyJoin();
  const posthog = usePostHog();

  function handleJoin() {
    if (isPending) return;
    posthog?.capture('study_join', { location: 'study_join_page' });
    joinStudy(target);
  }

  return (
    <>
      <VerticalStudyCard
        study={{
          name: data.name,
          imageUrl: data.imageUrl,
          description: data.description,
          memberCount: data.memberCount,
        }}
      />
      <Button
        variant="brandSolid"
        size="large"
        css={{ marginTop: tokens.spacing[6] }}
        onClick={handleJoin}
        disabled={isPending}
      >
        스터디 참여하기
      </Button>
    </>
  );
}
