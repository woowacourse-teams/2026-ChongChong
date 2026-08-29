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

export default function StudyJoinPage() {
  const [joinLink, handleJoinLink] = useInputState('');

  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={<TopHeader.Title>스터디 참여하기</TopHeader.Title>}
      />
      <Main>
        <form css={{ margin: `${tokens.spacing[5]} 0` }}>
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
              value={joinLink}
              onChange={handleJoinLink}
            />
          </Field>
          <Button
            css={{ marginTop: tokens.spacing[5] }}
            variant="brandSolid"
            size="large"
            disabled={isBlank(joinLink)}
          >
            스터디 참여하기
          </Button>
        </form>
      </Main>
    </Page>
  );
}
