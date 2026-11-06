package com.brasper.platform.accounting.interfaces.rest.resources;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DisposeFixedAssetResource(@NotNull LocalDate disposedOn) { }
