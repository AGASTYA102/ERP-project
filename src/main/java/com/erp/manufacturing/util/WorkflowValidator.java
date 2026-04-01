package com.erp.manufacturing.util;

import com.erp.manufacturing.enums.OrderStatus;

import java.util.Arrays;

public class WorkflowValidator {

    public static void validateTransition(OrderStatus current, OrderStatus... expected) {
        if (expected == null || expected.length == 0) return;
        boolean isValid = Arrays.asList(expected).contains(current);
        if (!isValid) {
            String expectedStr = expected.length == 1 ? expected[0].toString() : Arrays.toString(expected);
            throw new IllegalStateException("Invalid state transition: expected " + expectedStr + " but was " + current);
        }
    }

    private WorkflowValidator() {
        // Private constructor for utility class
    }
}
