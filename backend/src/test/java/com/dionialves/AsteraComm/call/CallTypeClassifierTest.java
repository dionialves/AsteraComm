package com.dionialves.AsteraComm.call;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallTypeClassifierTest {

    private CallTypeClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new CallTypeClassifier();
    }

    // ── FIXED_LOCAL (8 digits, starts with 2-8) ─────────────────────────────

    @Test
    void classify_shouldReturnFixedLocal_whenDstHas8DigitsStartingWithValidDigit() {
        assertThat(classifier.classify("33334444")).isEqualTo(CallType.FIXED_LOCAL); // starts with 3
        assertThat(classifier.classify("23334444")).isEqualTo(CallType.FIXED_LOCAL); // starts with 2
        assertThat(classifier.classify("83334444")).isEqualTo(CallType.FIXED_LOCAL); // starts with 8
    }

    @Test
    void classify_shouldReturnUnknown_when8DigitsStartingWithInvalidDigit() {
        assertThat(classifier.classify("13334444")).isEqualTo(CallType.UNKNOWN); // starts with 1
        assertThat(classifier.classify("03334444")).isEqualTo(CallType.UNKNOWN); // starts with 0
        assertThat(classifier.classify("93334444")).isEqualTo(CallType.UNKNOWN); // starts with 9 (mobile prefix)
    }

    // ── MOBILE_LOCAL (9 digits, starts with 9) ──────────────────────────────

    @Test
    void classify_shouldReturnMobileLocal_whenDstHas9DigitsStartingWith9() {
        assertThat(classifier.classify("933334444")).isEqualTo(CallType.MOBILE_LOCAL);
    }

    @Test
    void classify_shouldReturnUnknown_when9DigitsNotStartingWith9() {
        assertThat(classifier.classify("133334444")).isEqualTo(CallType.UNKNOWN); // starts with 1
        assertThat(classifier.classify("333334444")).isEqualTo(CallType.UNKNOWN); // starts with 3
    }

    // ── FIXED_LONG_DISTANCE (10 digits: DDD(2) + subscriber(8, starts 2-8)) ─

    @Test
    void classify_shouldReturnFixedLongDistance_whenDstHas10Digits_DddPlusFixed() {
        assertThat(classifier.classify("1133334444")).isEqualTo(CallType.FIXED_LONG_DISTANCE); // DDD=11
        assertThat(classifier.classify("4833334444")).isEqualTo(CallType.FIXED_LONG_DISTANCE); // DDD=48
    }

    @Test
    void classify_shouldReturnUnknown_when10DigitsWithSubscriberStartingWith9() {
        // 10 digits: DDD(2) + 9xxxxxxxx — subscriber of 8 digits can't start with 9
        assertThat(classifier.classify("1193334444")).isEqualTo(CallType.UNKNOWN);
    }

    // ── MOBILE_LONG_DISTANCE (11 digits, no leading 0: DDD(2) + subscriber(9, starts 9)) ─

    @Test
    void classify_shouldReturnMobileLongDistance_whenDstHas11Digits_NoPrefixDddPlusMobile() {
        assertThat(classifier.classify("11933334444")).isEqualTo(CallType.MOBILE_LONG_DISTANCE); // DDD=11
        assertThat(classifier.classify("48933334444")).isEqualTo(CallType.MOBILE_LONG_DISTANCE); // DDD=48
    }

    @Test
    void classify_shouldReturnUnknown_when11DigitsNoPrefixWithSubscriberNotStartingWith9() {
        // 11 digits, no leading 0, subscriber (digits[2..]) starts with 3 — invalid mobile
        assertThat(classifier.classify("11333334444")).isEqualTo(CallType.UNKNOWN);
    }

    // ── FIXED_LONG_DISTANCE (11 digits, leading 0: 0 + DDD(2) + subscriber(8, starts 2-8)) ─

    @Test
    void classify_shouldReturnFixedLongDistance_whenDstHas11Digits_ZeroPlusDddPlusFixed() {
        assertThat(classifier.classify("04833334444")).isEqualTo(CallType.FIXED_LONG_DISTANCE); // 0+48+33334444
        assertThat(classifier.classify("01133334444")).isEqualTo(CallType.FIXED_LONG_DISTANCE); // 0+11+33334444
    }

    @Test
    void classify_shouldReturnUnknown_when11DigitsLeadingZeroWithSubscriberStartingWith9() {
        // 0+DDD(2)+subscriber(8) but subscriber starts with 9 — invalid for fixed (8-digit subscriber)
        assertThat(classifier.classify("04893334444")).isEqualTo(CallType.UNKNOWN);
    }

    // ── MOBILE_LONG_DISTANCE (12 digits: 0 + DDD(2) + subscriber(9, starts 9)) ─

    @Test
    void classify_shouldReturnMobileLongDistance_whenDstHas12Digits_ZeroPlusDddPlusMobile() {
        assertThat(classifier.classify("048933334444")).isEqualTo(CallType.MOBILE_LONG_DISTANCE); // 0+48+933334444
        assertThat(classifier.classify("011933334444")).isEqualTo(CallType.MOBILE_LONG_DISTANCE); // 0+11+933334444
    }

    @Test
    void classify_shouldReturnUnknown_when12DigitsNotStartingWithZero() {
        assertThat(classifier.classify("119333344445")).isEqualTo(CallType.UNKNOWN);
    }

    @Test
    void classify_shouldReturnUnknown_when12DigitsLeadingZeroSubscriberNotStartingWith9() {
        // 0+DDD(2)+subscriber(9) but subscriber starts with 3 — invalid mobile
        assertThat(classifier.classify("048333334444")).isEqualTo(CallType.UNKNOWN);
    }

    // ── FIXED_LONG_DISTANCE (13 digits: 0 + CSP(2) + DDD(2) + subscriber(8, starts 2-8)) ─

    @Test
    void classify_shouldReturnFixedLongDistance_whenDstHas13Digits_ZeroCspDddPlusFixed() {
        // 0+14(CSP)+48(DDD)+33334444 = "0144833334444"
        assertThat(classifier.classify("0144833334444")).isEqualTo(CallType.FIXED_LONG_DISTANCE);
        // 0+21(CSP)+11(DDD)+33334444 = "0211133334444"
        assertThat(classifier.classify("0211133334444")).isEqualTo(CallType.FIXED_LONG_DISTANCE);
    }

    @Test
    void classify_shouldReturnUnknown_when13DigitsNotStartingWithZero() {
        assertThat(classifier.classify("1144833334444")).isEqualTo(CallType.UNKNOWN);
    }

    @Test
    void classify_shouldReturnUnknown_when13DigitsLeadingZeroSubscriberStartingWith9() {
        // 0+CSP(2)+DDD(2)+subscriber(8) but subscriber starts with 9 — invalid fixed
        assertThat(classifier.classify("0144893334444")).isEqualTo(CallType.UNKNOWN);
    }

    // ── MOBILE_LONG_DISTANCE (14 digits: 0 + CSP(2) + DDD(2) + subscriber(9, starts 9)) ─

    @Test
    void classify_shouldReturnMobileLongDistance_whenDstHas14Digits_ZeroCspDddPlusMobile() {
        // 0+14(CSP)+48(DDD)+933334444 = "01448933334444"
        assertThat(classifier.classify("01448933334444")).isEqualTo(CallType.MOBILE_LONG_DISTANCE);
        // 0+21(CSP)+11(DDD)+933334444 = "02111933334444"
        assertThat(classifier.classify("02111933334444")).isEqualTo(CallType.MOBILE_LONG_DISTANCE);
    }

    @Test
    void classify_shouldReturnUnknown_when14DigitsNotStartingWithZero() {
        assertThat(classifier.classify("11448933334444")).isEqualTo(CallType.UNKNOWN);
    }

    @Test
    void classify_shouldReturnUnknown_when14DigitsLeadingZeroSubscriberNotStartingWith9() {
        // 0+CSP(2)+DDD(2)+subscriber(9) but subscriber starts with 3 — invalid mobile
        assertThat(classifier.classify("01448333334444")).isEqualTo(CallType.UNKNOWN);
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test
    void classify_shouldReturnUnknown_whenDstHasFewerThan8Digits() {
        assertThat(classifier.classify("3333444")).isEqualTo(CallType.UNKNOWN);
    }

    @Test
    void classify_shouldReturnUnknown_whenDstHasMoreThan14Digits() {
        assertThat(classifier.classify("014489333344449")).isEqualTo(CallType.UNKNOWN);
    }

    @Test
    void classify_shouldStripNonNumericCharsBeforeClassifying() {
        assertThat(classifier.classify("11 9 3333-4444")).isEqualTo(CallType.MOBILE_LONG_DISTANCE);
        assertThat(classifier.classify("(11) 3333-4444")).isEqualTo(CallType.FIXED_LONG_DISTANCE);
        assertThat(classifier.classify("0 48 9 3333-4444")).isEqualTo(CallType.MOBILE_LONG_DISTANCE);
        assertThat(classifier.classify("0 14 48 3333-4444")).isEqualTo(CallType.FIXED_LONG_DISTANCE);
    }

    @Test
    void classify_shouldReturnUnknown_whenDstIsNullOrBlank() {
        assertThat(classifier.classify(null)).isEqualTo(CallType.UNKNOWN);
        assertThat(classifier.classify("")).isEqualTo(CallType.UNKNOWN);
        assertThat(classifier.classify("   ")).isEqualTo(CallType.UNKNOWN);
    }
}
