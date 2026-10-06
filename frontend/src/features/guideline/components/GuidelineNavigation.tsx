import { tokens, typography } from '../../../styles/global';
import leftArrow from '../../../shared/assets/left-arrow.svg';
import rightArrow from '../../../shared/assets/right-arrow.svg';
import { useGuidelineNavigation } from '../hooks/useGuidelineNavigation';

interface Props {
  page: number;
  pageCount: number;
  onPageChange: (page: number) => void;
}

const buttonStyle = {
  width: '44px',
  height: '44px',
  display: 'grid',
  placeItems: 'center',
  border: tokens.border.neutral,
  borderRadius: tokens.radius.full,
  background: tokens.bg.default,
  cursor: 'pointer',
  '&:hover': { background: tokens.bg.brandSubtle },
};

export default function GuidelineNavigation({ page, pageCount, onPageChange }: Props) {
  const { hasPrevious, hasNext, goPrevious, goNext } = useGuidelineNavigation(
    page,
    pageCount,
    onPageChange,
  );

  return (
    <nav
      aria-label="가이드 페이지 이동"
      css={{ display: 'grid', gridTemplateColumns: '44px 1fr 44px', alignItems: 'center' }}
    >
      <div>
        {hasPrevious && (
          <button type="button" aria-label="이전 가이드" css={buttonStyle} onClick={goPrevious}>
            <img src={leftArrow} width={20} height={20} alt="" />
          </button>
        )}
      </div>
      <span
        aria-label={`전체 ${pageCount}페이지 중 ${page}페이지`}
        css={{ ...typography.caption, textAlign: 'center', color: tokens.text.muted }}
      >
        <strong css={{ color: tokens.text.default }}>{page}</strong> / {pageCount}
      </span>
      <div>
        {hasNext && (
          <button type="button" aria-label="다음 가이드" css={buttonStyle} onClick={goNext}>
            <img src={rightArrow} width={20} height={20} alt="" />
          </button>
        )}
      </div>
    </nav>
  );
}
