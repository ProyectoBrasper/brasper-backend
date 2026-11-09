package com.brasper.platform.projects.interfaces.acl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The only way Billing, Expenses, Treasury and Reporting read projects. They keep the project id, nothing else. */
public interface ProjectsContextFacade {

    boolean existsProjectById(Long projectId);

    /** Empty when the code is unknown or not a valid project code. */
    Optional<Long> fetchProjectIdByCode(String code);

    /** The {@code ProjectStatus} name, e.g. {@code IN_PROGRESS}. */
    Optional<String> fetchProjectStatus(Long projectId);

    Optional<ProjectSnapshot> fetchProjectSnapshot(Long projectId);

    /** Zero-based page of projects ordered by id. */
    List<ProjectSnapshot> fetchProjectSnapshots(int page, int size);

    /** Progress earned in that month (not accumulated) per project id; projects without it are left out. */
    Map<Long, BigDecimal> fetchProgressByPeriod(int year, int month);
}
