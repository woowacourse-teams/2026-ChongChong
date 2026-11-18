import { fileTable } from '../../../shared/modules/files/mocks/db';

export async function createSeedAssignmentFiles() {
  const reports = [
    {
      id: 1001,
      ownerUserId: 1,
      name: '이든',
      summary: '스크린을 활용해 오픈 슛과 돌파 공간을 만듭니다.',
    },
    {
      id: 1002,
      ownerUserId: 5,
      name: '바니',
      summary: '빠른 수비 로테이션과 박스아웃으로 리바운드를 확보합니다.',
    },
  ];

  for (const report of reports) {
    const body = new TextEncoder().encode(
      `${report.name}의 농구 경기 분석\n\n${report.summary}\n`,
    ).buffer;
    await fileTable.create({
      id: report.id,
      ownerUserId: report.ownerUserId,
      originalFileName: `${report.name}_농구_경기_분석.txt`,
      contentType: 'text/plain',
      size: body.byteLength,
      uploadedSize: body.byteLength,
      body,
      status: 'COMPLETED',
    });
  }
}
