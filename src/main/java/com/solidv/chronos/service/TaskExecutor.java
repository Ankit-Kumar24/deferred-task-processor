package com.solidv.chronos.service;

import com.solidv.chronos.entity.DelayedTask;

public interface TaskExecutor {
    boolean execute(DelayedTask task);
}
