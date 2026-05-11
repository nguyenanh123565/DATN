package com.smarthome.event;

import org.springframework.context.ApplicationEvent;
import com.smarthome.entity.Product;

public class ProductChangedEvent extends ApplicationEvent {
    private final Product product;
    private final String action; // CREATE, UPDATE, DELETE

    public ProductChangedEvent(Object source, Product product, String action) {
        super(source);
        this.product = product;
        this.action = action;
    }

    public Product getProduct() {
        return product;
    }

    public String getAction() {
        return action;
    }
}
