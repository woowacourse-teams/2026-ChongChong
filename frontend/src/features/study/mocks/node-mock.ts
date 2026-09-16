import { userTable } from '../../user/mocks/db';
import { studyTable } from './db';
import { memberTable } from '../../member/mocks/db';

export async function setUpMockData() {
  const userId = 1;
  await userTable.create({
    id: userId,
    name: '벤지',
    profileImage: 'http://localhost:8000',
  });

  await studyTable.create({
    id: 1,
    name: '탄자니아 스터디',
    description: '탄자니아 출신 벤지와 함께하는 탄자니아 치안',
    inviteLink: 'tanzania',
  });
  await studyTable.create({
    id: 2,
    name: '농구 스터디',
    description: '2m 이든과 함께하는 농구 스터디',
    inviteLink: 'basketball',
  });

  await memberTable.create({
    id: 1,
    studyId: 1,
    userId: userId,
    name: '벤지',
    profileImage: 'http://localhost:8000',
    role: 'LEADER',
  });

  await memberTable.create({
    id: 2,
    studyId: 2,
    userId: userId,
    name: '벤지',
    profileImage: 'http://localhost:8000',
    role: 'MEMBER',
  });
}
