package dev.vality.hooker.service;

import dev.vality.damsel.webhooker.EventFilter;
import dev.vality.damsel.webhooker.WebhookParams;
import dev.vality.hooker.dao.HookDao;
import dev.vality.hooker.model.PartyMetadata;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HooksLimitService {
    private final HookDao hookDao;

    @Value("${limit.perShop}")
    private int defaultShopLimit;

    @Value("${limit.perParty}")
    private int defaultPartyLimit;

    public boolean isLimitExceeded(WebhookParams webhookParams) {
        EventFilter eventFilter = webhookParams.getEventFilter();
        if (!eventFilter.isSetInvoice()) {
            return false;
        }
        String partyId = webhookParams.getPartyRef().getId();
        PartyMetadata partyMetadata = hookDao.getPartyMetadata(partyId);
        String shopId = eventFilter.getInvoice().getShopRef().getId();
        if (shopId != null) {
            return isShopLimitExceeded(partyId, shopId, partyMetadata);
        } else {
            return isPartyLimitExceeded(partyId, partyMetadata);
        }
    }

    private boolean isPartyLimitExceeded(String partyId, PartyMetadata partyMetadata) {
        int limit = partyMetadata == null ? defaultPartyLimit : partyMetadata.getPartyLimit(defaultPartyLimit);
        int hooksCount = hookDao.getPartyHooksCount(partyId);
        return hooksCount >= limit;
    }

    private boolean isShopLimitExceeded(String partyId, String shopId, PartyMetadata partyMetadata) {
        int limit = partyMetadata == null ? defaultShopLimit : partyMetadata.getShopLimit(shopId, defaultShopLimit);
        int hooksCount = hookDao.getShopHooksCount(partyId, shopId);
        return hooksCount >= limit;
    }
}
