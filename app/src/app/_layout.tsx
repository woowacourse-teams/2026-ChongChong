import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { ActivityIndicator, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ActivityProvider } from '../features/activity/ActivityProvider';
import { EntryProvider } from '../features/entry/EntryProvider';
import { NoticeProvider } from '../features/notices/NoticeProvider';
import { ScenarioProvider } from '../mocks/ScenarioProvider';
import { AppHeader } from '../ui/AppHeader';
import { tokens as t } from '../ui/tokens';

export default function RootLayout() {
  const [loaded, error] = useFonts({
    Pretendard: require('../../assets/fonts/Pretendard-Regular.otf'),
    PretendardSemiBold: require('../../assets/fonts/Pretendard-SemiBold.otf'),
  });
  if (error) throw error;
  if (!loaded)
    return (
      <View style={{ flex: 1, justifyContent: 'center' }}>
        <ActivityIndicator accessibilityLabel="글꼴 준비 중" />
      </View>
    );
  return (
    <SafeAreaProvider>
      <ScenarioProvider>
        <EntryProvider>
          <NoticeProvider>
            <ActivityProvider>
              <StatusBar style="dark" />
              <Stack
                screenOptions={{
                  header: ({ navigation, options, route, back }) => (
                    <AppHeader
                      title={options.title ?? route.name}
                      {...(back ? { onBack: () => navigation.goBack() } : {})}
                    />
                  ),
                  contentStyle: { backgroundColor: t.color.background },
                }}
              >
                <Stack.Screen name="index" options={{ headerShown: false }} />
                <Stack.Screen name="login" options={{ headerShown: false }} />
                <Stack.Screen name="studies" options={{ headerShown: false }} />
                <Stack.Screen
                  name="create-study"
                  options={{ headerShown: false }}
                />
                <Stack.Screen
                  name="join-study"
                  options={{ headerShown: false }}
                />
                <Stack.Screen name="account" options={{ headerShown: false }} />
                <Stack.Screen
                  name="scenarios"
                  options={{ title: 'UI 시나리오' }}
                />
                <Stack.Screen name="showcase" options={{ title: '공통 UI' }} />
                <Stack.Screen name="study" options={{ headerShown: false }} />
                <Stack.Screen
                  name="notifications"
                  options={{ title: '알림' }}
                />
                <Stack.Screen name="notices" options={{ headerShown: false }} />
                <Stack.Screen
                  name="activity/[id]"
                  options={{ title: '상세' }}
                />
              </Stack>
            </ActivityProvider>
          </NoticeProvider>
        </EntryProvider>
      </ScenarioProvider>
    </SafeAreaProvider>
  );
}
