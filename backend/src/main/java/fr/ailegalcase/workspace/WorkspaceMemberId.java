package fr.ailegalcase.workspace;

import java.io.Serializable;
import java.util.UUID;

public class WorkspaceMemberId implements Serializable {
    private UUID workspace;
    private UUID user;

    public WorkspaceMemberId() {}

    public WorkspaceMemberId(UUID workspace, UUID user) {
        this.workspace = workspace;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkspaceMemberId other)) return false;
        return java.util.Objects.equals(workspace, other.workspace)
                && java.util.Objects.equals(user, other.user);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(workspace, user);
    }
}
