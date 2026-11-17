package withoutc.chongchong.file.repository;

import java.util.List;
import java.util.stream.Collectors;

public final class FileReferences {

    public static final List<Reference> ALL = List.of(
            new Reference("notice_attachments", "file_id"),
            new Reference("assignment_attachments", "file_id"),
            new Reference("submission_attachments", "file_id"),
            new Reference("studies", "image_file_id"),
            new Reference("users", "profile_image_file_id")
    );

    private FileReferences() {
    }

    static String notReferencedCondition(String fileAlias) {
        return ALL.stream()
                .map(reference -> "NOT EXISTS (SELECT 1 FROM %s ref WHERE ref.%s = %s.id)"
                        .formatted(reference.table(), reference.column(), fileAlias))
                .collect(Collectors.joining("\n  AND "));
    }

    public record Reference(String table, String column) {
    }
}
