package withoutc.chongchong.notice.controller;

final class NoticeApiExamples {

    static final String LEADER_NOTICE_LIST_RESPONSE = """
            {
              "nextCursor": null,
              "hasNext": false,
              "notices": [
                {
                  "id": 1,
                  "title": "이번 주 공지",
                  "content": "이번 주 스터디 일정을 확인해주세요.",
                  "createdAt": "2026-08-27T10:00:00",
                  "recipientCount": 5,
                  "readRecipientCount": 3,
                  "remindAt": "2026-08-28T10:00:00",
                  "isComplete": false
                }
              ]
            }
            """;

    static final String MEMBER_NOTICE_LIST_RESPONSE = """
            {
              "nextCursor": null,
              "hasNext": false,
              "notices": [
                {
                  "id": 1,
                  "title": "이번 주 공지",
                  "content": "이번 주 스터디 일정을 확인해주세요.",
                  "createdAt": "2026-08-27T10:00:00",
                  "isComplete": false
                }
              ]
            }
            """;

    private NoticeApiExamples() {
    }
}
