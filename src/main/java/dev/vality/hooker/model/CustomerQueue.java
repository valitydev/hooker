package dev.vality.hooker.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerQueue extends Queue {
    private String customerId;
}
