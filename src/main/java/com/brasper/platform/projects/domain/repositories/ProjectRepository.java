package com.brasper.platform.projects.domain.repositories;

import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import java.util.List;
import java.util.Optional;

/** Port used by the application layer. Implemented by Spring Data in {@code infrastructure}. */
public interface ProjectRepository {

    Project save(Project project);

    Optional<Project> findById(Long id);

    boolean existsByCode(ProjectCode code);

    List<Project> findAll();
}
