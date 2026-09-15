import { Tabs } from 'expo-router';
import { PlatformPressable } from 'expo-router/react-navigation';
import { View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { StudyHeader } from '../../features/home/StudyHeader';
import { StudyTabIcon } from '../../ui/StudyTabIcon';
import { tokens as t } from '../../ui/tokens';

export default function StudyLayout() {
  const insets = useSafeAreaInsets();
  return (
    <Tabs
      screenOptions={{
        header: () => <StudyHeader />,
        tabBarActiveTintColor: t.color.brand,
        tabBarInactiveTintColor: t.color.tertiary,
        tabBarLabelStyle: {
          fontFamily: t.font.regular,
          ...t.typography.caption,
          flexShrink: 0,
        },
        tabBarLabelPosition: 'below-icon',
        tabBarButton: ({ style, ...props }) => (
          <PlatformPressable {...props} style={[style, { padding: 0 }]} />
        ),
        tabBarStyle: {
          height: t.size.tabBar + insets.bottom,
          paddingTop: t.space.md,
          paddingHorizontal: t.size.tabHorizontalInset,
          paddingBottom: t.space.sm + insets.bottom,
          borderTopWidth: 0,
        },
        tabBarBackground: () => (
          <View
            pointerEvents="none"
            style={{
              flex: 1,
              backgroundColor: t.color.background,
              borderTopWidth: t.size.line,
              borderTopColor: t.color.border,
            }}
          />
        ),
        tabBarIconStyle: {
          width: t.size.tabIcon,
          height: t.size.tabIcon,
          marginBottom: t.space.xs,
        },
        tabBarActiveBackgroundColor: t.color.background,
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: '홈',
          tabBarAccessibilityLabel: '홈',
          tabBarIcon: ({ focused }) => (
            <StudyTabIcon name="home" focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="notices"
        options={{
          title: '공지',
          tabBarAccessibilityLabel: '공지',
          tabBarIcon: ({ focused }) => (
            <StudyTabIcon name="notices" focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="assignments"
        options={{
          title: '과제',
          tabBarAccessibilityLabel: '과제',
          tabBarIcon: ({ focused }) => (
            <StudyTabIcon name="assignments" focused={focused} />
          ),
        }}
      />
      <Tabs.Screen
        name="members"
        options={{
          title: '멤버',
          tabBarAccessibilityLabel: '멤버',
          tabBarIcon: ({ focused }) => (
            <StudyTabIcon name="members" focused={focused} />
          ),
        }}
      />
    </Tabs>
  );
}
