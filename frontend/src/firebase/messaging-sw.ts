import { getMessaging } from 'firebase/messaging/sw';
import { app } from './settingFCM';

getMessaging(app);
