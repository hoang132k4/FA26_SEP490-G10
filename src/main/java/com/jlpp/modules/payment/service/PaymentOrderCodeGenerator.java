package com.jlpp.modules.payment.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentOrderCodeGenerator {

    public String generateOrderCode() {
        return "JLPP" + UUID.randomUUID().toString().replace("-", "");
    }

    public String generateTransferContent(String orderCode) {
        return orderCode;
    }
}
