import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { ActivityIndicator, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { ScenarioProvider } from '../mocks/ScenarioProvider';
import { AppHeader } from '../ui/AppHeader';
import { tokens as t } from '../ui/tokens';

export const unstable_settings = { initialRouteName: 'index' };

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
          <Stack.Screen name="index" options={{ title: '총총' }} />
          <Stack.Screen name="showcase" options={{ title: '공통 UI' }} />
          <Stack.Screen name="study" options={{ title: '스터디' }} />
        </Stack>
      </ScenarioProvider>
    </SafeAreaProvider>
  );
}
