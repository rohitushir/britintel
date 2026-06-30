package com.companieswatch.events;

/**
 * The lender-relevant change types we classify and alert on (data-sources.md).
 * {@code priority} drives ordering/wording later; lower number = more urgent.
 */
public enum EventType {
    STATUS_CHANGE(1),       // company status moved (esp. toward liquidation/insolvency)
    CHARGE_CREATED(1),      // a new charge/secured debt registered
    CHARGE_SATISFIED(2),    // a charge marked satisfied
    OFFICER_APPOINTED(3),
    OFFICER_RESIGNED(3),
    ADDRESS_CHANGE(4),      // registered office address changed
    NEW_FILING(5);          // a new filing appeared (lower priority)

    private final int priority;

    EventType(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
