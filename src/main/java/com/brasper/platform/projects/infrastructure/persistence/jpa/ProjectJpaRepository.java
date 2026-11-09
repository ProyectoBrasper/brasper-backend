package com.brasper.platform.projects.infrastructure.persistence.jpa;

import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.repositories.ProjectRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectJpaRepository extends JpaRepository<Project, Long>, ProjectRepository {
}
