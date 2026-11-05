package com.brasper.platform.billing.application.commandservices;

import com.brasper.platform.billing.application.outboundservices.ProjectLookup;
import com.brasper.platform.billing.domain.model.aggregates.ScheduledCollection;
import com.brasper.platform.billing.domain.model.commands.CancelScheduledCollectionCommand;
import com.brasper.platform.billing.domain.model.commands.RescheduleCollectionCommand;
import com.brasper.platform.billing.domain.model.commands.ScheduleCollectionCommand;
import com.brasper.platform.billing.domain.repositories.ScheduledCollectionRepository;
import com.brasper.platform.shared.application.result.Result;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduledCollectionCommandService {

    private final ScheduledCollectionRepository collections;
    private final ProjectLookup projects;

    public ScheduledCollectionCommandService(ScheduledCollectionRepository collections, ProjectLookup projects) {
        this.collections = collections;
        this.projects = projects;
    }

    /** Any existing project will do: projecting a sale for a just-quoted project is normal. */
    @Transactional
    public Result<ScheduledCollection, ScheduledCollectionCommandFailure> handle(ScheduleCollectionCommand command) {
        if (projects.find(command.projectId()).isEmpty()) {
            return Result.failure(ScheduledCollectionCommandFailure.PROJECT_NOT_FOUND);
        }
        return Result.success(collections.save(new ScheduledCollection(command)));
    }

    @Transactional
    public Result<ScheduledCollection, ScheduledCollectionCommandFailure> handle(
            RescheduleCollectionCommand command) {
        return change(command.scheduledCollectionId(), collection -> collection.reschedule(command.expectedWeek()));
    }

    @Transactional
    public Result<ScheduledCollection, ScheduledCollectionCommandFailure> handle(
            CancelScheduledCollectionCommand command) {
        return change(command.scheduledCollectionId(), ScheduledCollection::cancel);
    }

    /** The aggregate throws IllegalStateException when the collection is already realized or cancelled. */
    private Result<ScheduledCollection, ScheduledCollectionCommandFailure> change(Long id,
            Consumer<ScheduledCollection> change) {
        ScheduledCollection collection = collections.findById(id).orElse(null);
        if (collection == null) {
            return Result.failure(ScheduledCollectionCommandFailure.NOT_FOUND);
        }
        try {
            change.accept(collection);
        } catch (IllegalStateException e) {
            return Result.failure(ScheduledCollectionCommandFailure.ALREADY_CLOSED);
        }
        return Result.success(collections.save(collection));
    }
}
