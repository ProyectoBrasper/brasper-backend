package com.brasper.platform.projects.application.commandservices;

import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.commands.CancelProjectCommand;
import com.brasper.platform.projects.domain.model.commands.CreateProjectCommand;
import com.brasper.platform.projects.domain.model.commands.RegisterProjectProgressCommand;
import com.brasper.platform.projects.domain.model.commands.StartProjectCommand;
import com.brasper.platform.projects.domain.model.commands.UpdateProjectCommand;
import com.brasper.platform.projects.domain.model.entities.ProgressRecord;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectStatus;
import com.brasper.platform.projects.domain.repositories.ClientRepository;
import com.brasper.platform.projects.domain.repositories.ProjectRepository;
import com.brasper.platform.shared.application.result.Result;
import java.math.BigDecimal;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectCommandService {

    private final ProjectRepository projects;
    private final ClientRepository clients;

    public ProjectCommandService(ProjectRepository projects, ClientRepository clients) {
        this.projects = projects;
        this.clients = clients;
    }

    @Transactional
    public Result<Project, ProjectCommandFailure> handle(CreateProjectCommand command) {
        if (clients.findById(command.clientId()).isEmpty()) {
            return Result.failure(ProjectCommandFailure.CLIENT_NOT_FOUND);
        }
        if (projects.existsByCode(command.code())) {
            return Result.failure(ProjectCommandFailure.DUPLICATE_CODE);
        }
        return Result.success(projects.save(new Project(command)));
    }

    @Transactional
    public Result<Project, ProjectCommandFailure> handle(UpdateProjectCommand command) {
        return change(command.projectId(), project -> project.update(command));
    }

    @Transactional
    public Result<Project, ProjectCommandFailure> handle(StartProjectCommand command) {
        return change(command.projectId(), Project::start);
    }

    @Transactional
    public Result<Project, ProjectCommandFailure> handle(CancelProjectCommand command) {
        return change(command.projectId(), Project::cancel);
    }

    /** The aggregate enforces these rules too; checking first tells the caller which one failed. */
    @Transactional
    public Result<ProgressRecord, ProjectCommandFailure> handle(RegisterProjectProgressCommand command) {
        Project project = projects.findById(command.projectId()).orElse(null);
        if (project == null) {
            return Result.failure(ProjectCommandFailure.PROJECT_NOT_FOUND);
        }
        if (project.getStatus() != ProjectStatus.IN_PROGRESS) {
            return Result.failure(ProjectCommandFailure.INVALID_STATUS_TRANSITION);
        }
        if (project.hasProgressFor(command.period())) {
            return Result.failure(ProjectCommandFailure.PERIOD_ALREADY_REGISTERED);
        }
        if (project.accumulatedProgress().value().add(command.percentage().value())
                .compareTo(BigDecimal.ONE) > 0) {
            return Result.failure(ProjectCommandFailure.PROGRESS_EXCEEDS_LIMIT);
        }
        project.registerProgress(command.period(), command.percentage());
        Project saved = projects.save(project);
        // Read the record back from the saved aggregate: that instance is the persisted one, with its id.
        return Result.success(saved.getProgressRecords().stream()
                .filter(record -> record.getPeriod().equals(command.period())).findFirst().orElseThrow());
    }

    /** The aggregate throws IllegalStateException when the status does not allow the change. */
    private Result<Project, ProjectCommandFailure> change(Long id, Consumer<Project> change) {
        Project project = projects.findById(id).orElse(null);
        if (project == null) {
            return Result.failure(ProjectCommandFailure.PROJECT_NOT_FOUND);
        }
        try {
            change.accept(project);
        } catch (IllegalStateException e) {
            return Result.failure(ProjectCommandFailure.INVALID_STATUS_TRANSITION);
        }
        return Result.success(projects.save(project));
    }
}
