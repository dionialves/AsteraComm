package com.dionialves.AsteraComm.call;

import org.springframework.stereotype.Component;

@Component
public class CallerIdParser {

    public String parse(String clid) {
        if (clid == null || clid.isBlank()) {
            return "";
        }
        int start = clid.indexOf('<');
        int end = clid.indexOf('>');
        if (start != -1 && end != -1 && end > start) {
            return clid.substring(start + 1, end).trim();
        }
        return clid.trim();
    }
}
