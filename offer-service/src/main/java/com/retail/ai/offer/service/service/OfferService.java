package com.retail.ai.offer.service.service;

import com.retail.ai.offer.service.model.Offer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OfferService {

    public List<Offer> getOffersForCart(String sessionId, int cartSize, double cartValue) {
        List<Offer> offers = new ArrayList<>();

        if (cartValue >= 75) {
            offers.add(Offer.builder()
                    .code("FREE_SHIPPING")
                    .title("Free shipping available")
                    .description("This cart already qualifies for free shipping.")
                    .type("shipping")
                    .build());
        }

        if (cartSize >= 2) {
            offers.add(Offer.builder()
                    .code("BUNDLE_SUGGESTION")
                    .title("Bundle and save time")
                    .description("Pair the items in this cart with a matching accessory to complete the setup.")
                    .type("bundle")
                    .build());
        }

        if (offers.isEmpty()) {
            offers.add(Offer.builder()
                    .code("CHECKOUT_REMINDER")
                    .title("Complete checkout while your cart is ready")
                    .description("No hard discount here. Just a nudge before the session goes cold.")
                    .type("message")
                    .build());
        }

        return offers;
    }
}
