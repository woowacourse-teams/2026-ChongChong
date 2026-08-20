import TopHeader from '../../../shared/ui/TopHeader';
import { useNavigate } from 'react-router';
import { tokens } from '../../../styles/global';
import backIcon from '../../../shared/assets/left-arrow.svg';
import { CSSProperties } from 'react';
import Main from '../../../shared/ui/Main';
import headerIcon from '../../../shared/assets/icons/header-icon.svg';
import StudyForm from '../components/StudyForm';

const pageStyle = {
  display: 'flex',
  flexDirection: 'column',
  minHeight: '100dvh',
  background: tokens.bg.default,
} satisfies CSSProperties;

const backButtonStyle = {
  display: 'grid',
  width: '32px',
  height: '32px',
  padding: 0,
  placeItems: 'center',
  border: 0,
  background: 'transparent',
  cursor: 'pointer',
} satisfies CSSProperties;

export function PrevButton() {
  const navigate = useNavigate();

  function goToPreviousPage() {
    // fallback 필요
    navigate(-1);
  }

  return (
    <button type="button" css={backButtonStyle} aria-label="뒤로 가기" onClick={goToPreviousPage}>
      <img src={backIcon} alt="" css={{ width: '24px', height: '24px' }} />
    </button>
  );
}

export default function NewStudyPage() {
  return (
    <div css={pageStyle}>
      <TopHeader
        left={<PrevButton />}
        middle={<div>스터디 만들기</div>}
        right={<a href="#">My</a>}
      />
      <Main
        css={{ padding: `0 ${tokens.spacing[4]} ${tokens.layout.gutter} ${tokens.spacing[5]}` }}
      >
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: `56px 0`,
          }}
        >
          <img src={headerIcon} alt="" css={{ width: '70px', height: '70px' }} />
        </div>
        <StudyForm />
      </Main>
    </div>
  );
}
