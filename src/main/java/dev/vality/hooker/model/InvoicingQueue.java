package dev.vality.hooker.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoicingQueue extends Queue {
    private String invoiceId;
}
