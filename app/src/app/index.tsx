import { router, usePathname } from 'expo-router';
import { useEffect } from 'react';
import { Image, StyleSheet, View } from 'react-native';
import { entryAssets } from '../features/entry/assets';
import { tokens as t } from '../ui/tokens';
export default function SplashScreen() {
  const pathname = usePathname();
  useEffect(() => {
    if (pathname !== '/') return;
    const timer = setTimeout(() => router.replace('/login'), 1200);
    return () => clearTimeout(timer);
  }, [pathname]);
  return (
    <View style={styles.screen}>
      <Image
        source={entryAssets.wordmark}
        style={styles.logo}
        resizeMode="contain"
        accessibilityLabel="총총"
      />
    </View>
  );
}
const styles = StyleSheet.create({
  screen: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: t.color.background,
  },
  logo: { width: 200, height: 100 },
});
