import Main from '../../../shared/ui/Main';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/widgets/PrevButton';
import { Field } from '../../../shared/ui/inputs/Field';
import Input from '../../../shared/ui/inputs/Input';
import Button from '../../../shared/ui/Button';
import { tokens } from '../../../styles/global';
import isBlank from '../../../shared/utils/isBlank';
import useInviteLinkState from '../hooks/useInviteLinkState';
import useValidateStudyInvite from '../hooks/useValidateStudyInvite';

export default function StudyJoinPage() {
  const [inviteLink, handleInviteLink] = useInviteLinkState();

  const { validateInvite, isPending, errorMessage, clearError } = useValidateStudyInvite();

  function handleJoinStudy(e: React.SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    void validateInvite(inviteLink);
  }

  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={<TopHeader.Title>초대링크로 스터디 참여하기</TopHeader.Title>}
      />
      <Main>
        <form css={{ margin: `${tokens.spacing[5]} 0` }} onSubmit={handleJoinStudy}>
          <Field>
            <Field.Label htmlFor={'study-join-link'} isRequired={true}>
              초대 링크
            </Field.Label>
            <Input
              id="study-join-link"
              placeholder="chongchong.app/welcome/join/15"
              value={inviteLink}
              onChange={(event) => {
                clearError();
                handleInviteLink(event);
              }}
              disabled={isPending}
              required={true}
            />
            <Field.SubText
              errorText={errorMessage}
              helpText={'스터디 리드에게 받은 초대 링크를 붙여넣어 주세요'}
            />
          </Field>
          <Button
            type="submit"
            css={{ marginTop: tokens.spacing[5] }}
            variant="brandSolid"
            size="large"
            disabled={isBlank(inviteLink) || isPending}
          >
            스터디 참여하기
          </Button>
        </form>
      </Main>
    </Page>
  );
}
