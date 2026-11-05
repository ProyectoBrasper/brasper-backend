package com.brasper.platform.projects.application.queryservices;

import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.entities.ProgressRecord;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectStatus;
import com.brasper.platform.projects.domain.repositories.ProjectRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectQueryService {

    private final ProjectRepository projects;

    public ProjectQueryService(ProjectRepository projects) {
        this.projects = projects;
    }

    /** Both filters are optional. shortcut: filters in memory, move them into the query if projects reach thousands. */
    public List<Project> findAll(ProjectStatus status, Long clientId) {
        return projects.findAll().stream()
                .filter(project -> status == null || project.getStatus() == status)
                .filter(project -> clientId == null || project.getClientId().equals(clientId))
                .toList();
    }

    public Optional<Project> findById(Long id) {
        return projects.findById(id);
    }

    /** Empty when the project does not exist; the list is ordered by month. */
    public Optional<List<ProgressRecord>> findProgress(Long projectId) {
        return projects.findById(projectId).map(project -> project.getProgressRecords().stream()
                .sorted(Comparator.comparingInt((ProgressRecord r) -> r.getPeriod().year())
                        .thenComparingInt(r -> r.getPeriod().month()))
                .toList());
    }
}
