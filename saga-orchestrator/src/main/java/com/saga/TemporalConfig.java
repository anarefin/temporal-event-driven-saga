package com.saga;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

@Configuration
public class TemporalConfig {

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker("ORDER_TASK_QUEUE");

        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);

        factory.start();
        return factory;
    }

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget("localhost:7233")
                .build());
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        return WorkflowClient.newInstance(workflowServiceStubs,
            WorkflowClientOptions.newBuilder()
                .setNamespace("default")
                .build());
    }
}

