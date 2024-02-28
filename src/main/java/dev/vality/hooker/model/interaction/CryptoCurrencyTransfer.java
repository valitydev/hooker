package dev.vality.hooker.model.interaction;

import dev.vality.damsel.base.Rational;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CryptoCurrencyTransfer implements UserInteraction {

    private String cryptoAddress;
    public Rational cryptoAmount;
    public String cryptoSymbolicCode;

}
