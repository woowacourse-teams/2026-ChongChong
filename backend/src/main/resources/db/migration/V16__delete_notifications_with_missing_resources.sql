-- Remove legacy logical notifications whose polymorphic resource has already been deleted.
DELETE FROM notifications
WHERE (
    resource_type = 'NOTICE'
    AND NOT EXISTS (
        SELECT 1
        FROM notices
        WHERE notices.id = notifications.resource_id
    )
) OR (
    resource_type = 'ASSIGNMENT'
    AND NOT EXISTS (
        SELECT 1
        FROM assignments
        WHERE assignments.id = notifications.resource_id
    )
) OR (
    resource_type = 'ASSIGNMENT_SUBMISSION'
    AND NOT EXISTS (
        SELECT 1
        FROM assignment_submissions
        WHERE assignment_submissions.id = notifications.resource_id
    )
);
