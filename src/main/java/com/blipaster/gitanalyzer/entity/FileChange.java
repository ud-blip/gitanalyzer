package com.blipaster.gitanalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.eclipse.jgit.diff.DiffEntry;

@Entity
@Table(name = "file_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class FileChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id")
    private Commit commit;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type")
    private DiffEntry.ChangeType changeType;

    private int additions = 0;

    private int deletions = 0;
}


