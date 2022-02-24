package dev.vality.hooker.utils;

import dev.vality.damsel.domain.Cash;
import dev.vality.damsel.domain.CashFlowAccount;
import dev.vality.damsel.domain.FinalCashFlowAccount;
import dev.vality.damsel.domain.FinalCashFlowPosting;
import dev.vality.damsel.domain.MerchantCashFlowAccount;
import dev.vality.damsel.domain.SystemCashFlowAccount;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CashFlowUtilsTest {
    @Test
    public void testFeeAmount() {
        List<FinalCashFlowPosting> finalCashFlowPosting = buildFinalCashFlowPostingList();
        Long feeAmount = CashFlowUtils.getFeeAmount(finalCashFlowPosting);
        Assert.assertEquals(feeAmount.longValue(), 20L);
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
