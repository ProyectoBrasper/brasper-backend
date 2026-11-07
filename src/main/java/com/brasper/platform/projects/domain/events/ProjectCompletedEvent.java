package com.brasper.platform.projects.domain.events;

import java.time.LocalDate;

/** Published when the last progress brings the project to 100%. */
public record ProjectCompletedEvent(Long projectId, LocalDate completedOn) { }
