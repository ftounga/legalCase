package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspace_members")
@IdClass(WorkspaceMemberId.class)
@Getter
@Setter
public class WorkspaceMember {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String memberRole;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
    }
}
