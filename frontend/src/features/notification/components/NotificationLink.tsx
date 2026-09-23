import { Link } from 'react-router';
import { useQuery } from '@tanstack/react-query';
import hasNotificationLogo from '../../../shared/assets/notification-green.png';
import notificationLogo from '../../../shared/assets/notification.svg';
import notificationQueries from '../queries';

export default function NotificationLink() {
  const { data: notificationData } = useQuery(notificationQueries.list());
  const hasUnreadNotification = notificationData?.some((data) => data.isRead === false);

  return (
    <Link
      to={'/notifications'}
      aria-label={hasUnreadNotification ? '알림, 읽지 않은 알림 있음' : '알림'}
    >
      {hasUnreadNotification ? (
        <img
          src={hasNotificationLogo}
          width={28}
          height={28}
          alt=""
          data-testid="has-unread-icon"
        />
      ) : (
        <img src={notificationLogo} width={28} height={28} alt="" data-testid="no-unread-icon" />
      )}
    </Link>
  );
}
