package com.example.voting.screens;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The base address the screens put in their callback URLs. The brief asks for exactly this: the
 * domain has to be changeable by configuration, because the emulator and a physical device do not
 * reach the server at the same address.
 */
@ConfigurationProperties("voting.screens")
record ScreenProperties(String baseUrl) {

    String url(String path) {
        return baseUrl + path;
    }
}
