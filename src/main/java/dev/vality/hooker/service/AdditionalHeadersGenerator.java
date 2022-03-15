package dev.vality.hooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdditionalHeadersGenerator {
    public static final String SIGNATURE_HEADER = "Content-Signature";

    public Map<String, String> generate(String signature) {
        return Map.of(SIGNATURE_HEADER, "alg=RS256; digest=" + signature);
    }
}
