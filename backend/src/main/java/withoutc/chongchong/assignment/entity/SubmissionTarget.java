package withoutc.chongchong.assignment.entity;

public enum SubmissionTarget {
    MEMBERS_ONLY,
    MEMBERS_AND_LEADER;

    public boolean requiresLeaderSubmission() {
        return this == MEMBERS_AND_LEADER;
    }
}
