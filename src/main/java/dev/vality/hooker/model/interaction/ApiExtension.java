package dev.vality.hooker.model.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiExtension implements UserInteraction {

    private String apiType;

}
