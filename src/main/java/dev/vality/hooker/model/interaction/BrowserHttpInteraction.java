package dev.vality.hooker.model.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class BrowserHttpInteraction implements UserInteraction {

    private String requestType;
    private String url;
    private Map<String, String> form;

}
