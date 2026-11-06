package com.brasper.platform.accounting.domain.model.commands;

import java.time.LocalDate;

public record DisposeFixedAssetCommand(Long fixedAssetId, LocalDate disposedOn) { }
