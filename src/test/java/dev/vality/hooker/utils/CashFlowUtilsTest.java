package dev.vality.hooker.utils;

import dev.vality.damsel.domain.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CashFlowUtilsTest {

    @Test
    void testFeeAmount() {
        List<FinalCashFlowPosting> finalCashFlowPosting = buildFinalCashFlowPostingList();
        Long feeAmount = CashFlowUtils.getFeeAmount(finalCashFlowPosting);
        assertEquals(20L, feeAmount.longValue());
    }

    private List<FinalCashFlowPosting> buildFinalCashFlowPostingList() {
        FinalCashFlowPosting firstFinalCashFlowPosting = new FinalCashFlowPosting();
        Cash cash = new Cash();
        cash.setAmount(10);
        firstFinalCashFlowPosting.setVolume(cash);
        firstFinalCashFlowPosting.setSource(new FinalCashFlowAccount()
                .setAccountType(CashFlowAccount.merchant(MerchantCashFlowAccount.settlement)));
        firstFinalCashFlowPosting.setDestination(
                new FinalCashFlowAccount().setAccountType(CashFlowAccount.system(SystemCashFlowAccount.settlement)));
        FinalCashFlowPosting secondFinalCashFlowPosting = firstFinalCashFlowPosting.deepCopy();
        return List.of(firstFinalCashFlowPosting, secondFinalCashFlowPosting);
    }
}
