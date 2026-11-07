import { useId, type CSSProperties, type ReactNode } from 'react';
import { useLocation } from 'react-router';
import { URL_POLICY } from '../policy/urlPolicy';
import usePageNavigation from '../hooks/usePageNavigation';
import { tokens, typography } from '../../styles/global';

interface Props {
  status: ReactNode;
  detail: ReactNode;
}

const tabListStyle = {
  display: 'flex',
  marginTop: tokens.spacing[5],
  borderBottom: tokens.border.neutral,
} satisfies CSSProperties;

const tabStyle = {
  ...typography.body,
  position: 'relative',
  flex: '1 0 auto',
  minWidth: 80,
  whiteSpace: 'nowrap',
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

export default function DetailTabs({ status, detail }: Props) {
  const location = useLocation();
  const { move } = usePageNavigation();
  const searchParams = new URLSearchParams(location.search);
  const activeTab: 'status' | 'detail' =
    searchParams.get('view') === 'status' ? 'status' : 'detail';
  const id = useId();
  const statusTabId = `${id}-status-tab`;
  const detailTabId = `${id}-detail-tab`;
  const statusPanelId = `${id}-status-panel`;
  const detailPanelId = `${id}-detail-panel`;

  return (
    <>
      <div role="tablist" aria-label="콘텐츠 정보" css={tabListStyle}>
        <button
          id={detailTabId}
          type="button"
          role="tab"
          aria-selected={activeTab === 'detail'}
          aria-controls={detailPanelId}
          css={{ ...tabStyle, ...(activeTab === 'detail' ? selectedTabStyle : {}) }}
          onClick={() => move(URL_POLICY.toContentTab(location, 'detail'))}
        >
          내용
        </button>
        <button
          id={statusTabId}
          type="button"
          role="tab"
          aria-selected={activeTab === 'status'}
          aria-controls={statusPanelId}
          css={{ ...tabStyle, ...(activeTab === 'status' ? selectedTabStyle : {}) }}
          onClick={() => move(URL_POLICY.toContentTab(location, 'status'))}
        >
          현황
        </button>
      </div>

      <div
        id={activeTab === 'detail' ? detailPanelId : statusPanelId}
        role="tabpanel"
        aria-labelledby={activeTab === 'detail' ? detailTabId : statusTabId}
        css={panelStyle}
      >
        {activeTab === 'detail' ? detail : status}
      </div>
    </>
  );
}
