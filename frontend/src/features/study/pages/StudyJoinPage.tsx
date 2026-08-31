import { useNavigate } from 'react-router';
import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import Field from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import { useInputState } from '../../../shared/hooks/useInputState';
import Button from '../../../shared/ui/Button';
import { tokens } from '../../../styles/global';
import isBlank from '../../../shared/utils/isBlank';
import useStudyJoin from '../hooks/useStudyJoin';

export default function StudyJoinPage() {
  const navigate = useNavigate();
  const [joinToken, handleJoinToken] = useInputState('');

  const { mutate: joinStudy, isPending } = useStudyJoin();

  function handleJoinStudy(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    joinStudy(
      { token: joinToken },
      {
        onSuccess: (data) => navigate(`/studies/${data.studyId}`),
      },
    );
  }

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
            // errorText={errors.name.message}
            // isError={errors.name.state}
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
