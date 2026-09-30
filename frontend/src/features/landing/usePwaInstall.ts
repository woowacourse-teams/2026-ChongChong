import { useRef, useState } from 'react';

type InstallChoice = { outcome: 'accepted' | 'dismissed'; platform: string };
type InstallPromptEvent = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<InstallChoice>;
};
type InstallResult = InstallChoice['outcome'] | 'installed' | 'unavailable' | 'busy';

let deferredPrompt: InstallPromptEvent | null = null;
let installedInSession = false;

// main.tsx imports the landing eagerly. Capture prompts before async bootstrap
// mounts the page, and retain them when navigating back to the landing.
window.addEventListener('beforeinstallprompt', (event) => {
  event.preventDefault();
  deferredPrompt = event as InstallPromptEvent;
  installedInSession = false;
});
window.addEventListener('appinstalled', () => {
  deferredPrompt = null;
  installedInSession = true;
});

function isStandalone() {
  return (
    window.matchMedia('(display-mode: standalone)').matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

export function usePwaInstall() {
  const [isPrompting, setIsPrompting] = useState(false);
  const prompting = useRef(false);

  async function install(): Promise<InstallResult> {
    if (isStandalone() || installedInSession) return 'installed';
    if (prompting.current) return 'busy';
    if (!deferredPrompt) return 'unavailable';

    const prompt = deferredPrompt;
    deferredPrompt = null;
    prompting.current = true;
    setIsPrompting(true);
    try {
      // Keep this call before the first await to preserve the user's activation.
      await prompt.prompt();
      return (await prompt.userChoice).outcome;
    } catch {
      return 'unavailable';
    } finally {
      prompting.current = false;
      setIsPrompting(false);
    }
  }

  return { install, isPrompting };
}
