package kg.megalab.ministorebackend.dto;

public record WorkspaceSection(SectionCode code, String title, boolean enabled,
                               boolean empty, String emptyMessage) {
}
