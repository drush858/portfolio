package dr.portfolio.market;

import org.springframework.messaging.simp.SimpMessagingTemplate;

//@Service
public class PriceBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final PriceCache priceCache;

    public PriceBroadcastService(
            SimpMessagingTemplate messagingTemplate,
            PriceCache priceCache) {
        this.messagingTemplate = messagingTemplate;
        this.priceCache = priceCache;
    }

    //@Scheduled(fixedRate = 5_000)
    public void broadcast() {
        messagingTemplate.convertAndSend(
                "/topic/prices",
                priceCache.getAll()
        );
    }
}
