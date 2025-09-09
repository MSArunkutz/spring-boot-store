package com.arun.store_api.payments;

import com.arun.store_api.orders.Order;
import com.arun.store_api.carts.CartEmptyException;
import com.arun.store_api.carts.CartNotFoundException;
import com.arun.store_api.carts.CartRepository;
import com.arun.store_api.orders.OrderRepository;
import com.arun.store_api.auth.AuthService;
import com.arun.store_api.carts.CartService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CheckoutService {
    public final CartRepository cartRepository;
    public final OrderRepository orderRepository;
    public final AuthService authService;
    public final CartService cartService;
    private final PaymentGateway paymentGateway;

    @Value("${websiteUrl}")
    private String websiteUrl;


    @Transactional
    public CheckoutResponse checkout(CheckoutRequest request) {
        var cart = cartRepository.getCartWithItems(request.getCartId()).orElse(null);
        if (cart == null) {
            throw new CartNotFoundException();
        }

        if (cart.getItems().isEmpty()) {
            throw new CartEmptyException();
        }

        var order = Order.fromCart(cart,authService.getCurrentUser());

        orderRepository.save(order);

        try{

            var session = paymentGateway.createCheckoutSession(order);

            cartService.clearCart(cart.getId());

            return new CheckoutResponse(order.getId(), session.getCheckoutUrl());
        }catch(PaymentException ex){

            orderRepository.delete(order);
            throw ex;
        }
    }

    public void handleWebhookEvent(WebhookRequest request) {
        paymentGateway
                .parseWebhookRequest(request)
                .ifPresent(paymentResult -> {
                    var order = orderRepository.findById(paymentResult.getOrderId()).orElseThrow();
                    order.setStatus(paymentResult.getPaymentStatus());
                    orderRepository.save(order);
                });
    }
}
