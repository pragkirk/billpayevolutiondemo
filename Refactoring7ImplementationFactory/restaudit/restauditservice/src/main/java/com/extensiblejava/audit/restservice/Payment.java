package com.kirkk.extensiblejava.audit.restservice;

public class Payment {
    private String payment;

    public String getPayment() {
        return payment;
    }

    public Payment setPayment(final String payment) {
        this.payment = payment;
        return this;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "payment='" + payment + '\'' +
                '}';
    }
}
