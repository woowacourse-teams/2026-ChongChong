package withoutc.chongchong.study.controller;

final class StudyApiExamples {

    static final String LEADER_STUDY_DETAIL_RESPONSE = """
            {
              "notices": {
                "count": 1,
                "items": [
                  {
                    "id": 1,
                    "title": "이번 주 공지",
                    "memberCount": 5,
                    "completeCount": 3
                  }
                ]
              },
              "assignments": {
                "count": 1,
                "items": [
                  {
                    "id": 1,
                    "title": "1주 차 과제",
                    "memberCount": 5,
                    "completeCount": 3
                  }
                ]
              }
            }
            """;

    static final String MEMBER_STUDY_DETAIL_RESPONSE = """
            {
              "totalCount": 2,
              "notices": {
                "items": [
                  {
                    "id": 1,
                    "title": "이번 주 공지"
                  }
                ]
              },
              "assignments": {
                "items": [
                  {
                    "id": 1,
                    "title": "1주 차 과제"
                  }
                ]
              }
            }
            """;

    private StudyApiExamples() {
    }
}
