package com.saga;

import io.temporal.workflow.Workflow;
import io.temporal.workflow.Saga;
import io.temporal.workflow.CompletablePromise;

public class OrderWorkflowImpl implements OrderWorkflow {

    private final CompletablePromise<Void> paymentCompleted = Workflow.newPromise();
    private final CompletablePromise<Void> inventoryReserved = Workflow.newPromise();
    private final CompletablePromise<Void> paymentFailed = Workflow.newPromise();
    private final CompletablePromise<Void> inventoryFailed = Workflow.newPromise();
    private final CompletablePromise<Void> shippingCompleted = Workflow.newPromise();
    private final CompletablePromise<Void> shippingFailed = Workflow.newPromise();

    @Override
    public void placeOrder(String orderId) {
        Saga.Options sagaOptions = new Saga.Options.Builder()
            .setContinueWithError(false)
            .build();
        Saga saga = new Saga(sagaOptions);

        try {
            // Register compensation handlers in reverse order of operations
            saga.addCompensation(this::compensatePayment);

            // Handle payment step
            Workflow.await(() -> paymentCompleted.isCompleted() || paymentFailed.isCompleted());
            if (paymentFailed.isCompleted()) {
                System.out.println("Payment failed for order: " + orderId + ", no compensation needed");
                return;
            }

            // Handle inventory step
            Workflow.await(() -> inventoryReserved.isCompleted() || inventoryFailed.isCompleted());
            if (inventoryFailed.isCompleted()) {
                System.out.println("Inventory reservation failed for order: " + orderId + ", compensating payment only");
                saga.compensate();
                return;
            }

            // After successful inventory, add inventory compensation
            saga.addCompensation(this::compensateInventoryReservation);

            // Handle shipping step
            Workflow.await(() -> shippingCompleted.isCompleted() || shippingFailed.isCompleted());
            if (shippingFailed.isCompleted()) {
                System.out.println("Shipping failed for order: " + orderId + ", initiating compensation");
                saga.compensate();
                return;
            }

            System.out.println("<<<< Order Confirmed and Shipped! >>>>");
        } catch (Exception e) {
            System.out.println("<<<< Order Failed! Compensating... >>>>");
            saga.compensate();
        }
    }

    private void compensatePayment() {
        System.out.println("<<<< Compensating Payment >>>>");
        // In a real implementation, you would call the payment service to refund
    }

    private void compensateInventoryReservation() {
        System.out.println("<<<< Compensating Inventory Reservation >>>>");
        // In a real implementation, you would call the inventory service to release the reservation
    }

    private void compensateShipping() {
        System.out.println("<<<< Compensating Shipping >>>>");
        // In a real implementation, you would call the shipping service to cancel shipping
    }

    @Override
    public void onPaymentCompleted() {
        System.out.println("<<<< onPaymentCompleted is called >>>>");
        paymentCompleted.complete(null);
    }

    @Override
    public void onPaymentFailed() {
        System.out.println("<<<< onPaymentFailed is called >>>>");
        paymentFailed.complete(null);
    }

    @Override
    public void onInventoryReserved() {
        System.out.println("<<<< onInventoryReserved is called >>>>");
        inventoryReserved.complete(null);
    }

    @Override
    public void onInventoryFailed() {
        System.out.println("<<<< onInventoryFailed is called >>>>");
        inventoryFailed.complete(null);
    }

    @Override
    public void onShippingCompleted() {
        System.out.println("<<<< onShippingCompleted is called >>>>");
        shippingCompleted.complete(null);
    }

    @Override
    public void onShippingFailed() {
        System.out.println("<<<< onShippingFailed is called >>>>");
        shippingFailed.complete(null);
    }
}
