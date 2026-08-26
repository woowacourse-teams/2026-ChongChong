import { CSSProperties } from 'react';
import Page from '../../../shared/ui/Page';
import TopHeader from '../../../shared/ui/TopHeader';
import { PrevButton } from '../../../shared/ui/components/PrevButton';
import Main from '../../../shared/ui/Main';
import BottomTab from '../../../shared/ui/components/BottomTab';
import UnderConstructionContent from '../../../shared/ui/UnderConstructionContent';

const emptyContentStyle = {
  alignItems: 'center',
  justifyContent: 'center',
} satisfies CSSProperties;

export default function NoConstructionPage() {
  return (
    <Page>
      <TopHeader
        left={<PrevButton />}
        middle={
          <>
            <TopHeader.Title>열심히 만들고 있어요.</TopHeader.Title>
          </>
        }
      />

      <Main css={emptyContentStyle}>
        <UnderConstructionContent message={'곧 만나요, 조금만 기다려주세요!'} />
      </Main>
      <BottomTab />
    </Page>
  );
}
