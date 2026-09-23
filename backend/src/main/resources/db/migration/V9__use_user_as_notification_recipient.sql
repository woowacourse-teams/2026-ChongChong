-- Notification recipients are user-owned rather than membership-owned.
-- Notification title, body, and deep link are stored as snapshots so notifications survive study/resource deletion.

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS fk60prjsdd6ahrlv3ayvjjqdlqi;

UPDATE notifications
SET recipient_id = (
    SELECT study_members.user_id
    FROM study_members
    WHERE study_members.id = notifications.recipient_id
);

ALTER TABLE notifications
    ADD CONSTRAINT fk60prjsdd6ahrlv3ayvjjqdlqi
        FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE notifications
    ADD COLUMN title VARCHAR(255);

ALTER TABLE notifications
    ADD COLUMN body TEXT;

ALTER TABLE notifications
    ADD COLUMN deep_link VARCHAR(255);

UPDATE notifications
SET title = CONCAT('[', COALESCE((SELECT studies.name
                                  FROM studies
                                  WHERE studies.id = notifications.study_id), '알 수 없는 스터디'), '] 새 공지'),
    body = COALESCE((SELECT notices.title
                     FROM notices
                     WHERE notices.id = notifications.resource_id), '공지')
WHERE resource_type = 'NOTICE';

UPDATE notifications
SET title = CONCAT('[', COALESCE((SELECT studies.name
                                  FROM studies
                                  WHERE studies.id = notifications.study_id), '알 수 없는 스터디'), '] 새 과제'),
    body = COALESCE((SELECT assignments.title
                     FROM assignments
                     WHERE assignments.id = notifications.resource_id), '과제')
WHERE resource_type = 'ASSIGNMENT';

UPDATE notifications
SET title = CONCAT('[', COALESCE((SELECT studies.name
                                  FROM studies
                                  WHERE studies.id = notifications.study_id), '알 수 없는 스터디'), '] 새 제출물'),
    body = COALESCE((SELECT CONCAT(study_members.name, ' 스터디원이 과제를 제출했어요')
                     FROM assignment_submissions
                              JOIN assignments ON assignments.id = assignment_submissions.assignment_id
                              JOIN study_members ON study_members.id = assignment_submissions.member_id
                     WHERE assignment_submissions.id = notifications.resource_id), '과제가 제출되었어요')
WHERE resource_type = 'ASSIGNMENT_SUBMISSION';

UPDATE notifications
SET deep_link = CONCAT('/studies/', notifications.study_id, '/notices/', notifications.resource_id)
WHERE resource_type = 'NOTICE';

UPDATE notifications
SET deep_link = CONCAT('/studies/', notifications.study_id, '/assignments/', notifications.resource_id)
WHERE resource_type = 'ASSIGNMENT';

UPDATE notifications
SET deep_link = CONCAT('/studies/', notifications.study_id, '/assignments/',
                       COALESCE((SELECT assignment_submissions.assignment_id
                                 FROM assignment_submissions
                                 WHERE assignment_submissions.id = notifications.resource_id), 0),
                       '/submissions/', notifications.resource_id)
WHERE resource_type = 'ASSIGNMENT_SUBMISSION';

ALTER TABLE notifications
    ALTER COLUMN title SET NOT NULL;

ALTER TABLE notifications
    ALTER COLUMN body SET NOT NULL;

ALTER TABLE notifications
    ALTER COLUMN deep_link SET NOT NULL;

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS notifications_resource_type_check;

ALTER TABLE notifications
    ADD CONSTRAINT notifications_resource_type_check
        CHECK (resource_type IN ('NOTICE', 'ASSIGNMENT', 'ASSIGNMENT_SUBMISSION'));

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS fko5m57o40ivnn0td7m511dx42k;

ALTER TABLE notifications
    DROP COLUMN study_id;
