-- Limit study-scoped assignment lookups before checking submission state.
CREATE INDEX idx_assignments_study_id ON assignments (study_id);
