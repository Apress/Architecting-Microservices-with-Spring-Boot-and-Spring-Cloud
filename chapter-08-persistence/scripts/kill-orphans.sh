#!/bin/bash

# Kill orphaned Spring Boot services that may still be running
# Usage: ./kill-orphans.sh [--dry-run]

DRY_RUN=false
if [ "$1" = "--dry-run" ]; then
    DRY_RUN=true
    echo "DRY RUN - no processes will be killed"
    echo ""
fi

SERVICES=("config-server" "eureka-server" "api-gateway" "user-management-service" "charge-unit-management-service" "session-management-service" "notification-management-service")
FOUND=0

echo "Checking for orphaned Spring Boot services..."
echo "=============================================="

for service in "${SERVICES[@]}"; do
    PIDS=$(pgrep -f "spring-boot.*$service" 2>/dev/null)
    if [ -z "$PIDS" ]; then
        PIDS=$(pgrep -f "$service.*spring-boot" 2>/dev/null)
    fi
    if [ -z "$PIDS" ]; then
        PIDS=$(pgrep -f "target/classes.*$service" 2>/dev/null)
    fi

    if [ -n "$PIDS" ]; then
        for PID in $PIDS; do
            FOUND=$((FOUND + 1))
            if [ "$DRY_RUN" = true ]; then
                echo "  [WOULD KILL] $service (PID: $PID)"
            else
                echo "  Killing $service (PID: $PID)..."
                kill $PID 2>/dev/null
            fi
        done
    fi
done

# Also check for any maven spring-boot:run processes
MAVEN_PIDS=$(pgrep -f "mvn.*spring-boot:run" 2>/dev/null)
if [ -n "$MAVEN_PIDS" ]; then
    for PID in $MAVEN_PIDS; do
        FOUND=$((FOUND + 1))
        if [ "$DRY_RUN" = true ]; then
            echo "  [WOULD KILL] mvn spring-boot:run (PID: $PID)"
        else
            echo "  Killing mvn spring-boot:run (PID: $PID)..."
            kill $PID 2>/dev/null
        fi
    done
fi

echo ""
if [ $FOUND -eq 0 ]; then
    echo "No orphaned services found."
else
    if [ "$DRY_RUN" = true ]; then
        echo "Found $FOUND orphaned process(es). Run without --dry-run to kill them."
    else
        echo "Killed $FOUND orphaned process(es)."
        # Clean up stale PID files
        cd "$(dirname "$0")/.."
        rm -f logs/*.pid 2>/dev/null
    fi
fi
