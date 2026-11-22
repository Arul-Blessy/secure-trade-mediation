package com.trade.trademediation.enums;

public enum OrderStatus {
    NOT_YET_DISPATCHED,
    IN_TRANSIT,
    DELIVERED,
    COMPLETED,
    CANCELLED_BY_BUYER,
    CANCELLED_BY_SELLER,
    RETURN_REQUESTED,
    RETURN_APPROVED,
    RETURN_REJECTED
}