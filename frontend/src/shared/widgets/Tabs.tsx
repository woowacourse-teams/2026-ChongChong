import { useId, type CSSProperties, type ReactNode } from 'react';
import { useSearchParams } from 'react-router';
import { tokens, typography } from '../../styles/global';

type TabItem = { key: string; label: string; panel: ReactNode };

type TabItemList = readonly [TabItem, ...TabItem[]];

interface Props {
  tabs: TabItemList;
}

const tabListStyle = {
  display: 'grid',
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

const TAB_QUERY_NAME = 'tab';

function useQueryTabs(tabs: TabItemList) {
  const defaultTab = tabs[0];
  const [searchParams, setSearchParams] = useSearchParams({
    tab: defaultTab.key,
  });
  const requestedTab = searchParams.get(TAB_QUERY_NAME);

  const selectTab = (key: (typeof tabs)[number]['key']) => {
    setSearchParams((currentParams) => {
      const nextParams = new URLSearchParams(currentParams);
      nextParams.set(TAB_QUERY_NAME, key);
      return nextParams;
    });
  };

  const activeTab = tabs.find(({ key }) => key === requestedTab) ?? defaultTab;
  return { activeTab, selectTab };
}

export default function Tabs({ tabs }: Props) {
  const { activeTab, selectTab } = useQueryTabs(tabs);
  const id = useId();

  return (
    <>
      <div
        role="tablist"
        aria-label="콘텐츠 정보"
        css={{
          ...tabListStyle,
          gridTemplateColumns: `repeat(${tabs.length}, minmax(0, 1fr))`,
        }}
      >
        {tabs.map(({ key, label }) => {
          const isActive = activeTab.key === key;
          return (
            <button
              id={`${id}-${key}-tab`}
              key={key}
              type="button"
              role="tab"
              aria-selected={isActive}
              aria-controls={`${id}-${key}-panel`}
              css={{ ...tabStyle, ...(isActive ? selectedTabStyle : {}) }}
              onClick={() => selectTab(key)}
            >
              {label}
            </button>
          );
        })}
      </div>
      <div
        id={`${id}-${activeTab.key}-panel`}
        role="tabpanel"
        aria-labelledby={`${id}-${activeTab.key}-tab`}
        css={panelStyle}
      >
        {activeTab.panel}
      </div>
    </>
  );
}
