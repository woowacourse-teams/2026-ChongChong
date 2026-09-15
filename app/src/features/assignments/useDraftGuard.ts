import { useNavigation } from 'expo-router';
import {
  type NavigationAction,
  usePreventRemove,
} from 'expo-router/react-navigation';
import { useRef, useState } from 'react';
export function useDraftGuard(dirty: boolean) {
  const navigation = useNavigation();
  const allow = useRef(false);
  const pending = useRef<NavigationAction | null>(null);
  const [discard, setDiscard] = useState(false);
  usePreventRemove(dirty, ({ data }) => {
    if (allow.current) navigation.dispatch(data.action);
    else {
      pending.current = data.action;
      setDiscard(true);
    }
  });
  return {
    discard,
    cancel: () => setDiscard(false),
    allowLeave: () => {
      allow.current = true;
    },
    confirm: () => {
      allow.current = true;
      setDiscard(false);
      if (pending.current) navigation.dispatch(pending.current);
    },
  };
}
