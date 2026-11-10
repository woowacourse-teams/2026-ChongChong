import { ChangeEventHandler } from 'react';
import { tokens, typography } from '../../../styles/global';
import Input from '../../../shared/ui/inputs/Input';
import Button from '../../../shared/ui/Button';
import searchIcon from '../../../shared/assets/search.svg';
import searchEmptyIcon from '../../../shared/assets/icons/search-empty.webp';
import { Paragraph } from '../../../shared/ui/Typography';
import { CSSObject } from '@emotion/react';

interface SearchProps {
  onSubmit: React.SubmitEventHandler<HTMLFormElement>;
  searchValue: string;
  changeSearchValue: ChangeEventHandler<HTMLInputElement>;
}

const formStyle = {
  display: 'flex',
  gap: tokens.spacing[2],
  marginBottom: tokens.spacing[4],
};

const inputStyle = {
  ...typography.body,
  minHeight: 40,
  height: 40,
  borderRadius: 20,
  padding: `0 ${tokens.spacing[5]}`,
};

const iconStyle = {
  width: 40,
  height: 40,
  flexShrink: 0,
  padding: 0,
  borderRadius: 20,
};

export function Search({ onSubmit, searchValue, changeSearchValue, ...props }: SearchProps) {
  return (
    <form role="search" aria-label="공개 스터디 검색" onSubmit={onSubmit} css={formStyle}>
      <Input
        type="search"
        aria-label="스터디 이름 검색"
        placeholder="스터디 이름 검색"
        value={searchValue}
        onChange={changeSearchValue}
        css={inputStyle}
        {...props}
      />
      <Button type="submit" variant="brandSolid" size="small" aria-label="검색" css={iconStyle}>
        <img src={searchIcon} alt="" width={20} height={20} />
      </Button>
    </form>
  );
}

const searchEmptyContentContainerStyle = {
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  gap: tokens.spacing[1],
} satisfies CSSObject;

export function SearchEmptyContent() {
  return (
    <div role="status" css={searchEmptyContentContainerStyle}>
      <img
        src={searchEmptyIcon}
        width={300}
        height={300}
        alt=""
        css={{ maxWidth: '100%', height: 'auto' }}
      />
      <Paragraph variant="sectionLabel" css={{ color: tokens.text.muted }}>
        검색 결과가 없어요
      </Paragraph>
    </div>
  );
}
