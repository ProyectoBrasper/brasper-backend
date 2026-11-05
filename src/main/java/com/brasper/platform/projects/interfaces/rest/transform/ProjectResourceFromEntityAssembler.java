package com.brasper.platform.projects.interfaces.rest.transform;

import com.brasper.platform.projects.domain.model.aggregates.Project;
import com.brasper.platform.projects.interfaces.rest.resources.ProjectResource;

public final class ProjectResourceFromEntityAssembler {

    private ProjectResourceFromEntityAssembler() {
    }

    public static ProjectResource toResourceFromEntity(Project project) {
        return new ProjectResource(project.getId(), project.getCode().value(), project.getName(),
                project.getDescription(), project.getClientId(), project.getBudget().amount(),
                project.getBudget().currency().name(), project.getQuotedOn(), project.getStatus().name(),
                project.getParticipation().value(), project.getSalesCommissionRate().value());
    }
}
