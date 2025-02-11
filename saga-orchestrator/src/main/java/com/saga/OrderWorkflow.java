package com.saga;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.SignalMethod;

@WorkflowInterface
public interface OrderWorkflow {

    @WorkflowMethod
    void placeOrder(String orderId);

    @SignalMethod
    void onPaymentCompleted();

    @SignalMethod
    void onPaymentFailed();

    @SignalMethod
    void onInventoryReserved();

    @SignalMethod
    void onInventoryFailed();

    @SignalMethod
    void onShippingCompleted();

    @SignalMethod
    void onShippingFailed();
}

