package com.masrdelivery.exception;

import com.masrdelivery.money.Money;

/** Wallet does not hold enough to pay; the wallet is left untouched. */
public class InsufficientBalanceException extends PaymentException {
    private static final long serialVersionUID = 1L;
    private final Money balance;
    private final Money required;

    public InsufficientBalanceException(Money balance, Money required) {
        super("Insufficient wallet balance: you have " + balance + " but " + required + " is required (short by "
                + required.minus(balance) + ").");
        this.balance = balance;
        this.required = required;
    }
    public Money balance()  { return balance; }
    public Money required() { return required; }
}
