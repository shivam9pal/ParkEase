package com.parkease.parkinglot.entity;

/**
 * Approval status for parking lots submitted by managers.
 *
 * - PENDING: Awaiting admin review - APPROVED: Lot is approved and visible to
 * drivers - REJECTED: Lot was rejected with reason provided
 */
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}
