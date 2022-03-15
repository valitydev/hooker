package dev.vality.hooker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CustomerMessage extends Message {
    private CustomerMessageEnum type;
    private String bindingId;

    public boolean isBinding() {
        return type == CustomerMessageEnum.BINDING;
    }
}
