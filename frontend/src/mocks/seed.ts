import { createSeedUsers } from '../features/user/mocks/db';
import { createSeedStudies } from '../features/study/mocks/db';
import { createSeedMembers } from '../features/member/mocks/db';
import { createSeedAssignments } from '../features/assignment/mocks/db';
import { createSeedSubmissions } from '../features/assignment/mocks/db';
import { createSeedNotices } from '../features/notice/mocks/db';
import { createSeedNotification } from '../features/notification/mocks/db';

export function seedDevelopmentData() {
  createSeedUsers();
  createSeedStudies();
  createSeedMembers();
  createSeedAssignments();
  createSeedSubmissions();
  createSeedNotices();
  createSeedNotification();
}
