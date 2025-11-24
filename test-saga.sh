#!/bin/bash

# Temporal Event-Driven Saga Pattern - Automated Test Script
# This script helps you test various success and failure scenarios

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ORDER_SERVICE_URL="http://localhost:8081"
TEMPORAL_UI_URL="http://localhost:8233"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Function to print colored messages
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo ""
    echo -e "${GREEN}================================${NC}"
    echo -e "${GREEN}$1${NC}"
    echo -e "${GREEN}================================${NC}"
    echo ""
}

# Function to check if services are running
check_services() {
    print_header "Checking Services Status"
    
    services=("order-service" "payment-service" "inventory-service" "shipping-service" "saga-orchestrator" "kafka" "temporal")
    
    for service in "${services[@]}"; do
        if docker-compose ps | grep -q "$service.*Up"; then
            print_success "$service is running"
        else
            print_error "$service is NOT running"
            return 1
        fi
    done
    
    echo ""
    print_success "All services are running!"
}

# Function to wait for services to be ready
wait_for_services() {
    print_info "Waiting for services to be ready..."
    sleep 5
}

# Function to send order request
send_order() {
    local order_id=$1
    local customer_id=${2:-"CUST-DEFAULT"}
    
    print_info "Sending order: $order_id (Customer: $customer_id)"
    
    if [ -n "$customer_id" ]; then
        response=$(curl -s -X POST "${ORDER_SERVICE_URL}/orders/${order_id}?customerId=${customer_id}")
    else
        response=$(curl -s -X POST "${ORDER_SERVICE_URL}/orders/${order_id}")
    fi
    
    echo "Response: $response"
    echo ""
}

# Function to monitor logs
monitor_logs() {
    local service=$1
    local duration=${2:-5}
    
    print_info "Monitoring $service logs for $duration seconds..."
    timeout $duration docker-compose logs -f $service 2>/dev/null || true
    echo ""
}

# Function to update service configuration
update_service_config() {
    local service=$1
    local property=$2
    local value=$3
    
    print_info "Updating $service configuration: $property=$value"
    
    # Update application.properties
    local props_file="${PROJECT_ROOT}/${service}/src/main/resources/application.properties"
    
    if [ -f "$props_file" ]; then
        # Create backup
        cp "$props_file" "${props_file}.bak"
        
        # Update property
        if grep -q "^${property}=" "$props_file"; then
            sed -i.tmp "s/^${property}=.*/${property}=${value}/" "$props_file"
            rm -f "${props_file}.tmp"
        else
            echo "${property}=${value}" >> "$props_file"
        fi
        
        print_success "Configuration updated. Rebuilding $service..."
        docker-compose up -d --build $service
        wait_for_services
    else
        print_error "Configuration file not found: $props_file"
        return 1
    fi
}

# Function to restore service configuration
restore_service_config() {
    local service=$1
    local props_file="${PROJECT_ROOT}/${service}/src/main/resources/application.properties"
    local backup_file="${props_file}.bak"
    
    if [ -f "$backup_file" ]; then
        print_info "Restoring $service configuration..."
        mv "$backup_file" "$props_file"
        docker-compose up -d --build $service
        wait_for_services
        print_success "$service configuration restored"
    fi
}

# Function to view Kafka events
view_kafka_events() {
    local topic=$1
    
    print_info "Viewing Kafka events from topic: $topic"
    print_info "Press Ctrl+C to stop..."
    
    docker exec -it temporal-event-driven-saga-kafka-1 kafka-console-consumer \
        --bootstrap-server localhost:9092 \
        --topic $topic \
        --from-beginning 2>/dev/null || print_error "Failed to connect to Kafka"
}

# Test Scenario 1: Complete Success Flow
test_success_scenario() {
    print_header "TEST 1: Complete Success Flow (Happy Path)"
    
    print_info "Configuration: All services set to SUCCESS"
    update_service_config "payment-service" "payment.simulation.success" "true"
    update_service_config "inventory-service" "inventory.simulation.success" "true"
    update_service_config "shipping-service" "shipping.simulation.success" "true"
    
    send_order "ORDER-SUCCESS-001" "CUST-SUCCESS"
    
    print_info "Expected: Payment ✅ → Inventory ✅ → Shipping ✅"
    monitor_logs "saga-orchestrator" 10
    
    print_success "Test completed! Check Temporal UI: ${TEMPORAL_UI_URL}"
    print_info "Workflow ID: ORDER-SUCCESS-001"
    echo ""
}

# Test Scenario 2: Payment Failure
test_payment_failure() {
    print_header "TEST 2: Payment Failure (No Compensation)"
    
    print_info "Configuration: Payment set to FAIL"
    update_service_config "payment-service" "payment.simulation.success" "false"
    
    send_order "ORDER-PAYMENT-FAIL-001" "CUST-FAIL"
    
    print_info "Expected: Payment ❌ → No Compensation"
    monitor_logs "saga-orchestrator" 10
    
    print_success "Test completed! Check Temporal UI: ${TEMPORAL_UI_URL}"
    print_info "Workflow ID: ORDER-PAYMENT-FAIL-001"
    echo ""
    
    # Restore configuration
    restore_service_config "payment-service"
}

# Test Scenario 3: Inventory Failure with Compensation
test_inventory_failure() {
    print_header "TEST 3: Inventory Failure (Payment Compensation)"
    
    print_info "Configuration: Payment SUCCESS, Inventory FAIL"
    update_service_config "payment-service" "payment.simulation.success" "true"
    update_service_config "inventory-service" "inventory.simulation.success" "false"
    
    send_order "ORDER-INVENTORY-FAIL-001" "CUST-COMP"
    
    print_info "Expected: Payment ✅ → Inventory ❌ → Compensate Payment 🔄"
    monitor_logs "saga-orchestrator" 10
    
    print_success "Test completed! Check Temporal UI: ${TEMPORAL_UI_URL}"
    print_info "Workflow ID: ORDER-INVENTORY-FAIL-001"
    echo ""
    
    # Restore configuration
    restore_service_config "inventory-service"
}

# Test Scenario 4: Shipping Failure with Full Compensation
test_shipping_failure() {
    print_header "TEST 4: Shipping Failure (Full Compensation)"
    
    print_info "Configuration: Payment SUCCESS, Inventory SUCCESS, Shipping FAIL"
    update_service_config "payment-service" "payment.simulation.success" "true"
    update_service_config "inventory-service" "inventory.simulation.success" "true"
    update_service_config "shipping-service" "shipping.simulation.success" "false"
    
    send_order "ORDER-SHIPPING-FAIL-001" "CUST-FULLCOMP"
    
    print_info "Expected: Payment ✅ → Inventory ✅ → Shipping ❌ → Compensate All 🔄"
    monitor_logs "saga-orchestrator" 10
    
    print_success "Test completed! Check Temporal UI: ${TEMPORAL_UI_URL}"
    print_info "Workflow ID: ORDER-SHIPPING-FAIL-001"
    echo ""
    
    # Restore configuration
    restore_service_config "shipping-service"
}

# Function to restore all configurations
restore_all_configs() {
    print_header "Restoring All Configurations to Default"
    
    restore_service_config "payment-service"
    restore_service_config "inventory-service"
    restore_service_config "shipping-service"
    
    print_success "All configurations restored to default (SUCCESS mode)"
}

# Function to display menu
show_menu() {
    clear
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  Saga Pattern Testing Script${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "Test Scenarios:"
    echo "  1) Complete Success Flow"
    echo "  2) Payment Failure (No Compensation)"
    echo "  3) Inventory Failure (Partial Compensation)"
    echo "  4) Shipping Failure (Full Compensation)"
    echo "  5) Run All Tests Sequentially"
    echo ""
    echo "Utilities:"
    echo "  6) Check Services Status"
    echo "  7) Monitor Saga Orchestrator Logs"
    echo "  8) View Kafka Events (order-events)"
    echo "  9) View Kafka Events (payment-events)"
    echo "  10) Open Temporal UI"
    echo "  11) Restore All Configurations"
    echo ""
    echo "  0) Exit"
    echo ""
}

# Function to open Temporal UI
open_temporal_ui() {
    print_info "Opening Temporal UI in browser..."
    
    if command -v open &> /dev/null; then
        open "${TEMPORAL_UI_URL}"
    elif command -v xdg-open &> /dev/null; then
        xdg-open "${TEMPORAL_UI_URL}"
    else
        print_info "Please open manually: ${TEMPORAL_UI_URL}"
    fi
}

# Function to run all tests
run_all_tests() {
    print_header "Running All Test Scenarios"
    
    test_success_scenario
    sleep 3
    
    test_payment_failure
    sleep 3
    
    test_inventory_failure
    sleep 3
    
    test_shipping_failure
    sleep 3
    
    print_header "All Tests Completed!"
    print_success "Check Temporal UI for all workflow executions: ${TEMPORAL_UI_URL}"
    
    restore_all_configs
}

# Main script logic
main() {
    # Check if Docker Compose is available
    if ! command -v docker-compose &> /dev/null; then
        print_error "docker-compose is not installed or not in PATH"
        exit 1
    fi
    
    # Check if running from project root
    if [ ! -f "docker-compose.yaml" ]; then
        print_error "Please run this script from the project root directory"
        exit 1
    fi
    
    # Interactive menu
    while true; do
        show_menu
        read -p "Enter your choice [0-11]: " choice
        
        case $choice in
            1)
                test_success_scenario
                read -p "Press Enter to continue..."
                ;;
            2)
                test_payment_failure
                read -p "Press Enter to continue..."
                ;;
            3)
                test_inventory_failure
                read -p "Press Enter to continue..."
                ;;
            4)
                test_shipping_failure
                read -p "Press Enter to continue..."
                ;;
            5)
                run_all_tests
                read -p "Press Enter to continue..."
                ;;
            6)
                check_services
                read -p "Press Enter to continue..."
                ;;
            7)
                print_info "Monitoring saga-orchestrator logs (Press Ctrl+C to stop)"
                docker-compose logs -f saga-orchestrator
                ;;
            8)
                view_kafka_events "order-events"
                ;;
            9)
                view_kafka_events "payment-events"
                ;;
            10)
                open_temporal_ui
                read -p "Press Enter to continue..."
                ;;
            11)
                restore_all_configs
                read -p "Press Enter to continue..."
                ;;
            0)
                print_info "Exiting..."
                exit 0
                ;;
            *)
                print_error "Invalid choice. Please try again."
                sleep 2
                ;;
        esac
    done
}

# Run main function
main

