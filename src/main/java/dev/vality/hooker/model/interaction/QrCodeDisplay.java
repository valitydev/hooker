package dev.vality.hooker.model.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QrCodeDisplay implements UserInteraction {

    private byte[] qrCode;

}
