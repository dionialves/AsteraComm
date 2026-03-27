package com.dionialves.AsteraComm.call;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelParserTest {

    private ChannelParser parser;

    @BeforeEach
    void setUp() {
        parser = new ChannelParser();
    }

    @Test
    void parse_shouldExtractCode_forPjsipChannel() {
        assertThat(parser.parse("PJSIP/4933401714-000045f0")).isEqualTo("4933401714");
    }

    @Test
    void parse_shouldExtractCode_forSipChannel() {
        assertThat(parser.parse("SIP/100001-abcd1234")).isEqualTo("100001");
    }

    @Test
    void parse_shouldExtractCode_forDahdiChannel() {
        assertThat(parser.parse("DAHDI/1-xyz")).isEqualTo("1");
    }

    @Test
    void parse_shouldExtractCode_whenSuffixIsLong() {
        assertThat(parser.parse("PJSIP/5511999990000-0000ffff")).isEqualTo("5511999990000");
    }

    @Test
    void parse_shouldReturnEmpty_whenChannelIsNull() {
        assertThat(parser.parse(null)).isEmpty();
    }

    @Test
    void parse_shouldReturnEmpty_whenChannelIsBlank() {
        assertThat(parser.parse("")).isEmpty();
        assertThat(parser.parse("   ")).isEmpty();
    }

    @Test
    void parse_shouldReturnEmpty_whenChannelHasNoSlash() {
        assertThat(parser.parse("PJSIP")).isEmpty();
    }

    @Test
    void parse_shouldReturnEmpty_whenChannelHasNoSuffix() {
        // sem hífen — retorna a parte inteira após a barra
        assertThat(parser.parse("PJSIP/4933401714")).isEqualTo("4933401714");
    }

    @Test
    void parse_shouldBeProtocolAgnostic() {
        assertThat(parser.parse("IAX2/mypeer-00001")).isEqualTo("mypeer");
        assertThat(parser.parse("SCCP/1234-00abc")).isEqualTo("1234");
    }
}
