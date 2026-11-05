package com.brasper.platform.billing.infrastructure.acl;

import com.brasper.platform.billing.application.outboundservices.ProjectLookup;
import com.brasper.platform.projects.interfaces.acl.ProjectsContextFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ProjectsFacadeLookup implements ProjectLookup {

    private final ProjectsContextFacade projects;

    ProjectsFacadeLookup(ProjectsContextFacade projects) {
        this.projects = projects;
    }

    @Override
    public Optional<BillableProject> find(Long projectId) {
        return projects.fetchProjectSnapshot(projectId)
                .map(snapshot -> new BillableProject(snapshot.id(), snapshot.clientId(), snapshot.status()));
    }
}
