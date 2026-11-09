import { Paragraph } from '../../../shared/ui/Typography';
import { tokens } from '../../../styles/global';
import searchEmptyIcon from '../assets/search-empty.png?width=600&as=webp';

export default function StudySearchEmptyContent() {
  return (
    <div
      role="status"
      css={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: tokens.spacing[1],
      }}
    >
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
