package com.banka1.credit_service.domain.enums;

import lombok.Getter;

@Getter
public enum Status {
    PENDING(true), APPROVED(true), DECLINED(true), ACTIVE(false), OVERDUE(false), PAID_OFF(false);
    final boolean loanRequest;

    Status(boolean loanRequest) {
        this.loanRequest = loanRequest;
    }


}
