package com.dionialves.AsteraComm.call;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallerIdParserTest {

    private CallerIdParser parser;

    @BeforeEach
    void setUp() {
        parser = new CallerIdParser();
    }

    @Test
    void parse_shouldExtractNumber_whenFullFormat() {
        String result = parser.parse("\"João Silva\" <5511999990000>");
        assertThat(result).isEqualTo("5511999990000");
    }

    @Test
    void parse_shouldExtractNumber_whenAngleBracketsOnly() {
        String result = parser.parse("<5511999990000>");
        assertThat(result).isEqualTo("5511999990000");
    }

    @Test
    void parse_shouldReturnNumberAsIs_whenPureNumber() {
        String result = parser.parse("5511999990000");
        assertThat(result).isEqualTo("5511999990000");
    }

    @Test
    void parse_shouldReturnEmpty_whenNullOrBlank() {
        assertThat(parser.parse(null)).isEmpty();
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("   ")).isEmpty();
    }
}
