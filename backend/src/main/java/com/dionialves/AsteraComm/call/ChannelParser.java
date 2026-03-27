package com.dionialves.AsteraComm.call;

import org.springframework.stereotype.Component;

@Component
public class ChannelParser {

    public String parse(String channel) {
        if (channel == null || channel.isBlank()) {
            return "";
        }
        int slashIndex = channel.indexOf('/');
        if (slashIndex == -1) {
            return "";
        }
        String afterSlash = channel.substring(slashIndex + 1);
        int dashIndex = afterSlash.indexOf('-');
        if (dashIndex == -1) {
            return afterSlash;
        }
        return afterSlash.substring(0, dashIndex);
    }
}
