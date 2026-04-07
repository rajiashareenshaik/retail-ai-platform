package com.retail.ai.offer.service.api;

import com.retail.ai.offer.service.model.Offer;
import com.retail.ai.offer.service.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping("/cart/{sessionId}")
    public List<Offer> getOffers(@PathVariable String sessionId,
                                 @RequestParam(defaultValue = "0") int cartSize,
                                 @RequestParam(defaultValue = "0") double cartValue) {
        return offerService.getOffersForCart(sessionId, cartSize, cartValue);
    }
}
