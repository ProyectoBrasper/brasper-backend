package com.brasper.platform.expenses.infrastructure.acl;

import com.brasper.platform.expenses.application.outboundservices.ExpenseProjectLookup;
import com.brasper.platform.projects.interfaces.acl.ProjectsContextFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ExpenseProjectsFacadeLookup implements ExpenseProjectLookup {

    private final ProjectsContextFacade projects;

    ExpenseProjectsFacadeLookup(ProjectsContextFacade projects) {
        this.projects = projects;
    }

    @Override
    public Optional<String> findStatus(Long projectId) {
        return projects.fetchProjectStatus(projectId);
    }
}
