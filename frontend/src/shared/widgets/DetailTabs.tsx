import { useId, useState, type CSSProperties, type ReactNode } from 'react';
import { tokens, typography } from '../../styles/global';

type Tab = 'summary' | 'detail';

interface Props {
  summary: ReactNode;
  detail: ReactNode;
}

const tabListStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
  marginTop: tokens.spacing[5],
  borderBottom: tokens.border.neutral,
} satisfies CSSProperties;

const tabStyle = {
  ...typography.body,
  position: 'relative',
  minHeight: 44,
  padding: `${tokens.spacing[2]} ${tokens.spacing[3]}`,
  border: 0,
  background: 'transparent',
  color: tokens.text.placeholder,
  cursor: 'pointer',
} satisfies CSSProperties;

const selectedTabStyle = {
  color: tokens.text.brand,
  boxShadow: `inset 0 -2px 0 ${tokens.bg.brand}`,
} satisfies CSSProperties;

const panelStyle = {
  display: 'flex',
  flexDirection: 'column',
  paddingTop: tokens.spacing[5],
} satisfies CSSProperties;

export default function DetailTabs({ summary, detail }: Props) {
  const [activeTab, setActiveTab] = useState<Tab>('summary');
  const id = useId();
  const summaryTabId = `${id}-summary-tab`;
  const detailTabId = `${id}-detail-tab`;
  const summaryPanelId = `${id}-summary-panel`;
  const detailPanelId = `${id}-detail-panel`;

  return (
    <>
      <div role="tablist" aria-label="콘텐츠 정보" css={tabListStyle}>
        <button
          id={summaryTabId}
          type="button"
          role="tab"
          aria-selected={activeTab === 'summary'}
          aria-controls={summaryPanelId}
          css={{ ...tabStyle, ...(activeTab === 'summary' ? selectedTabStyle : {}) }}
          onClick={() => setActiveTab('summary')}
        >
          요약
        </button>
        <button
          id={detailTabId}
          type="button"
          role="tab"
          aria-selected={activeTab === 'detail'}
          aria-controls={detailPanelId}
          css={{ ...tabStyle, ...(activeTab === 'detail' ? selectedTabStyle : {}) }}
          onClick={() => setActiveTab('detail')}
        >
          상세
        </button>
      </div>

      <div
        id={activeTab === 'summary' ? summaryPanelId : detailPanelId}
        role="tabpanel"
        aria-labelledby={activeTab === 'summary' ? summaryTabId : detailTabId}
        css={panelStyle}
      >
        {activeTab === 'summary' ? summary : detail}
      </div>
    </>
  );
}
