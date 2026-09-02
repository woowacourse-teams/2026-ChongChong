-- Older JPA-created schemas may still contain the writer relationship.
-- The final schema treats every notice and assignment as leader-authored,
-- so content has no StudyMember dependency.

ALTER TABLE assignments
    DROP CONSTRAINT IF EXISTS fk7ek80ftj5dsyq1uwib7nvfpkn;
ALTER TABLE assignments
    DROP COLUMN IF EXISTS writer_id;

ALTER TABLE notices
    DROP CONSTRAINT IF EXISTS fkom02f3y50va5rg7h3lslrcscv;
ALTER TABLE notices
    DROP COLUMN IF EXISTS writer_id;

-- All study-owned data and member-scoped activity is disposable.

ALTER TABLE study_members
    DROP CONSTRAINT IF EXISTS fkb8cp6e23p040p7ml6sswen5cs;
ALTER TABLE study_members
    ADD CONSTRAINT fkb8cp6e23p040p7ml6sswen5cs
        FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE CASCADE;

ALTER TABLE study_members
    DROP CONSTRAINT IF EXISTS fkdt5hp8mbe53a5sdcecsf7wpyg;
ALTER TABLE study_members
    ADD CONSTRAINT fkdt5hp8mbe53a5sdcecsf7wpyg
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE assignments
    DROP CONSTRAINT IF EXISTS fkqe029wvx6pjp0q8tlypilk0c9;
ALTER TABLE assignments
    ADD CONSTRAINT fkqe029wvx6pjp0q8tlypilk0c9
        FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE CASCADE;

ALTER TABLE assignment_reminders
    DROP CONSTRAINT IF EXISTS fk23if7m1gm235fcgs3hleq5do2;
ALTER TABLE assignment_reminders
    ADD CONSTRAINT fk23if7m1gm235fcgs3hleq5do2
        FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE;

ALTER TABLE assignment_submissions
    DROP CONSTRAINT IF EXISTS fkm7i7ubgh7y2n6mvg8muw62oax;
ALTER TABLE assignment_submissions
    ADD CONSTRAINT fkm7i7ubgh7y2n6mvg8muw62oax
        FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE;

ALTER TABLE assignment_submissions
    DROP CONSTRAINT IF EXISTS fknsfnkdmpdvd605vnpkpt1g0md;
ALTER TABLE assignment_submissions
    ADD CONSTRAINT fknsfnkdmpdvd605vnpkpt1g0md
        FOREIGN KEY (member_id) REFERENCES study_members (id) ON DELETE CASCADE;

ALTER TABLE notices
    DROP CONSTRAINT IF EXISTS fk403omqxfm0hkwwx6trtd12u76;
ALTER TABLE notices
    ADD CONSTRAINT fk403omqxfm0hkwwx6trtd12u76
        FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE CASCADE;

ALTER TABLE notice_recipients
    DROP CONSTRAINT IF EXISTS fky3a9r7igh6bsqigv2lkgmu6o;
ALTER TABLE notice_recipients
    ADD CONSTRAINT fky3a9r7igh6bsqigv2lkgmu6o
        FOREIGN KEY (notice_id) REFERENCES notices (id) ON DELETE CASCADE;

ALTER TABLE notice_recipients
    DROP CONSTRAINT IF EXISTS fkpm6u1t0n3px52tld2apx6oess;
ALTER TABLE notice_recipients
    ADD CONSTRAINT fkpm6u1t0n3px52tld2apx6oess
        FOREIGN KEY (member_id) REFERENCES study_members (id) ON DELETE CASCADE;

ALTER TABLE notice_reminders
    DROP CONSTRAINT IF EXISTS fko0h19ha7jyrdtym2iy97ip03n;
ALTER TABLE notice_reminders
    ADD CONSTRAINT fko0h19ha7jyrdtym2iy97ip03n
        FOREIGN KEY (notice_id) REFERENCES notices (id) ON DELETE CASCADE;

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS fko5m57o40ivnn0td7m511dx42k;
ALTER TABLE notifications
    ADD CONSTRAINT fko5m57o40ivnn0td7m511dx42k
        FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE CASCADE;

ALTER TABLE notifications
    DROP CONSTRAINT IF EXISTS fk60prjsdd6ahrlv3ayvjjqdlqi;
ALTER TABLE notifications
    ADD CONSTRAINT fk60prjsdd6ahrlv3ayvjjqdlqi
        FOREIGN KEY (recipient_id) REFERENCES study_members (id) ON DELETE CASCADE;
