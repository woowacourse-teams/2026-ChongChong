import { ErrorBoundary, getErrorMessage } from 'react-error-boundary';
import useIntegerParams from '../../../shared/hooks/useIntegerParams';
import { typography } from '../../../styles/global';
import ErrorContent from '../../../shared/ui/ErrorContent';
import MemberList from './MemberList';
import InviteStudyLinkBox, { InviteStudyLinkBoxFallback } from './InviteStudyLinkBox';

function LeaderContent() {
  const { studyId } = useIntegerParams(['studyId']);

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <MemberList.Leader />
        </ErrorBoundary>
        <ErrorBoundary
          fallbackRender={({ error }) => (
            <InviteStudyLinkBoxFallback message={getErrorMessage(error)} />
          )}
        >
          <InviteStudyLinkBox studyId={studyId} />
        </ErrorBoundary>
      </section>
    </>
  );
}

function MemberContent() {
  const { studyId } = useIntegerParams(['studyId']);

  return (
    <>
      <section>
        <h2 css={typography.subtitle}>스터디 멤버</h2>
        <ErrorBoundary
          fallbackRender={({ error }) => <ErrorContent message={getErrorMessage(error)} />}
        >
          <MemberList.Member />
        </ErrorBoundary>
        <ErrorBoundary
          fallbackRender={({ error }) => (
            <InviteStudyLinkBoxFallback message={getErrorMessage(error)} />
          )}
        >
          <InviteStudyLinkBox studyId={studyId} />
        </ErrorBoundary>
      </section>
    </>
  );
}

const MemberListContent = {
  Leader: LeaderContent,
  Member: MemberContent,
};

export default MemberListContent;
