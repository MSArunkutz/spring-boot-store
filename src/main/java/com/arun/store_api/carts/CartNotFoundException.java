package com.arun.store_api.carts;

public class CartNotFoundException extends RuntimeException{
    public CartNotFoundException() {
        super("Cart is empty");
    }
}
