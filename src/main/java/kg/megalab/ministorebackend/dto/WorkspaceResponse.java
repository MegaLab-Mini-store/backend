package kg.megalab.ministorebackend.dto;

import java.util.List;

public record WorkspaceResponse(String cashier, boolean storeEmpty,
                                List<WorkspaceSection> sections) {
}
