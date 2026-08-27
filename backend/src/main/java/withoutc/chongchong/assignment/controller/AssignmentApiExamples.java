package withoutc.chongchong.assignment.controller;

final class AssignmentApiExamples {

    static final String LEADER_ASSIGNMENT_LIST_RESPONSE = """
            {
              "nextCursor": null,
              "hasNext": false,
              "assignments": [
                {
                  "id": 1,
                  "title": "1주 차 과제",
                  "content": "이번 주 과제를 제출해주세요.",
                  "submissionMethod": "링크 제출",
                  "closeAt": "2026-08-29T23:59:00",
                  "memberCount": 5,
                  "completeCount": 3,
                  "remindAt": "2026-08-28T10:00:00",
                  "isComplete": false
                }
              ]
            }
            """;

    static final String MEMBER_ASSIGNMENT_LIST_RESPONSE = """
            {
              "nextCursor": null,
              "hasNext": false,
              "assignments": [
                {
                  "id": 1,
                  "title": "1주 차 과제",
                  "content": "이번 주 과제를 제출해주세요.",
                  "submissionMethod": "링크 제출",
                  "closeAt": "2026-08-29T23:59:00",
                  "isComplete": false
                }
              ]
            }
            """;

    private AssignmentApiExamples() {
    }
}
