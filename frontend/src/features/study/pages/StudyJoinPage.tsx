import { useNavigate, useSearchParams } from 'react-router';
import { useMemo } from 'react';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import { useInputState } from '../../../shared/hooks/useInputState';
import Button from '../../../shared/ui/Button';
import { tokens } from '../../../styles/global';
import { ValidationError } from '../../../shared/api/error';
import isBlank from '../../../shared/utils/isBlank';
import useStudyJoin from '../hooks/useStudyJoin';
import { usePostHog } from '@posthog/react';

export default function StudyJoinPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [joinToken, handleJoinToken] = useInputState(() => {
    const token = searchParams.get('token');
    if (token) {
      return token;
    }
    return '';
  });

  const posthog = usePostHog();

  const { mutate: joinStudy, error, isPending } = useStudyJoin();

  function handleJoinStudy(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();

    posthog?.capture('study_join', {
      location: 'study_join_page',
    });

    joinStudy(
      { token: joinToken },
      {
        onSuccess: (data) => navigate(`/studies/${data.studyId}`),
      },
    );
  }

  const fieldErrors = useMemo(
    () => (error instanceof ValidationError ? error.fieldErrors : {}),
    [error],
  );

  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={<TopHeader.Title>스터디 참여하기</TopHeader.Title>}
      />
      <Main>
        <form css={{ margin: `${tokens.spacing[5]} 0` }} onSubmit={handleJoinStudy}>
          <Field
            id="study-join-link"
            isRequired={true}
            label="초대 링크"
            helpText="스터디 리드에게 받은 초대 링크를 붙여넣어 주세요"
            isError={Boolean(fieldErrors.token)}
            errorText={fieldErrors.token}
          >
            <Input
              id="study-join-link"
              placeholder="chongchong.app/welcome/join/15"
              value={joinToken}
              onChange={handleJoinToken}
            />
          </Field>
          <Button
            type="submit"
            css={{ marginTop: tokens.spacing[5] }}
            variant="brandSolid"
            size="large"
            disabled={isBlank(joinToken) || isPending}
          >
            스터디 참여하기
          </Button>
        </form>
      </Main>
    </Page>
  );
}
