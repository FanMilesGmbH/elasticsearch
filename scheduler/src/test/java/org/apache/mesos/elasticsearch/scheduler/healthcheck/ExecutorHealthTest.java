package org.apache.mesos.elasticsearch.scheduler.healthcheck;

import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.apache.mesos.elasticsearch.scheduler.state.ESTaskStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests executor health
 */
public class ExecutorHealthTest {
    @Mock
    private Scheduler scheduler;
    @Mock
    private SchedulerDriver schedulerDriver;
    @Mock
    private ESTaskStatus taskStatus;
    private ExecutorHealth executorHealth;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        Protos.TaskStatus defaultStatus = Protos.TaskStatus.getDefaultInstance().getDefaultInstanceForType();
        when(taskStatus.getStatus()).thenReturn(defaultStatus);
        executorHealth = new ExecutorHealth(scheduler, schedulerDriver, taskStatus, 1L);
    }

    @Test
    public void timeoutShouldNotResetWhenChecking() {
        Double lastUpdate = runAndGetLastUpdate(executorHealth);
        when(taskStatus.getStatus()).thenReturn(overdueTaskStatus());
        Double thisUpdate = runAndGetLastUpdate(executorHealth);
        assertEquals(lastUpdate, thisUpdate);
    }

    private Protos.TaskStatus overdueTaskStatus() {
        return Protos.TaskStatus.newBuilder().setTaskId(Protos.TaskID.newBuilder().setValue("")).setState(Protos.TaskState.TASK_RUNNING).setTimestamp(10L).build();
    }

    @Test
    public void shouldCallExecutorLostWhenTimeout() {
        runAndGetLastUpdate(executorHealth);
        when(taskStatus.getStatus()).thenReturn(overdueTaskStatus());
        runAndGetLastUpdate(executorHealth);
        verify(scheduler, times(1)).executorLost(eq(schedulerDriver), any(), any(), anyInt());
    }

    private Double runAndGetLastUpdate(ExecutorHealth executorHealth) {
        executorHealth.run();
        return executorHealth.getLastUpdate();
    }
}