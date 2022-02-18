package dev.vality.hooker.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Queue {
    private long id;
    private Hook hook;
}
