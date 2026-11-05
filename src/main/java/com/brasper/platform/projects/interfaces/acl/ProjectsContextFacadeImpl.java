package com.brasper.platform.projects.interfaces.acl;

import com.brasper.platform.projects.application.queryservices.ClientQueryService;
import com.brasper.platform.projects.application.queryservices.ProjectQueryService;
import com.brasper.platform.projects.domain.model.aggregates.Client;
import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.domain.model.valueobjects.ProjectCode;
import com.brasper.platform.shared.domain.model.valueobjects.Period;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Lives in {@code interfaces} (the UML puts it in {@code application}) because the layer rules forbid
 * application from depending on interfaces, where the facade contract lives.
 */
@Service
public class ProjectsContextFacadeImpl implements ProjectsContextFacade {

    private final ProjectQueryService projects;
    private final ClientQueryService clients;

    public ProjectsContextFacadeImpl(ProjectQueryService projects, ClientQueryService clients) {
        this.projects = projects;
        this.clients = clients;
    }

    @Override
    public boolean existsProjectById(Long projectId) {
        return projects.findById(projectId).isPresent();
    }

    @Override
    public Optional<Long> fetchProjectIdByCode(String code) {
        try {
            return projects.findByCode(new ProjectCode(code)).map(Project::getId);
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> fetchProjectStatus(Long projectId) {
        return projects.findById(projectId).map(project -> project.getStatus().name());
    }

    @Override
    public Optional<ProjectSnapshot> fetchProjectSnapshot(Long projectId) {
        return projects.findById(projectId).map(this::snapshot);
    }

    /** shortcut: pages in memory after loading all projects, push it into the query if they reach thousands. */
    @Override
    public List<ProjectSnapshot> fetchProjectSnapshots(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be >= 0 and size >= 1");
        }
        return projects.findAll(null, null).stream().sorted(Comparator.comparing(Project::getId))
                .skip((long) page * size).limit(size).map(this::snapshot).toList();
    }

    @Override
    public Map<Long, BigDecimal> fetchProgressByPeriod(int year, int month) {
        return projects.findProgressByPeriod(new Period(year, month));
    }

    private ProjectSnapshot snapshot(Project project) {
        String clientName = clients.findById(project.getClientId()).map(Client::getBusinessName).orElse(null);
        return new ProjectSnapshot(project.getId(), project.getCode().value(), project.getName(),
                project.getClientId(), clientName, project.getStatus().name(), project.getBudget().amount(),
                project.getBudget().currency().name(), project.getParticipation().value(), project.getSalesCommissionRate().value(),
                project.accumulatedProgress().value(), project.estimatedDirectCost().amount());
    }
}
