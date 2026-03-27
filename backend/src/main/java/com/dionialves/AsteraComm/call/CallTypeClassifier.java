package com.dionialves.AsteraComm.call;

import org.springframework.stereotype.Component;

@Component
public class CallTypeClassifier {

    public CallType classify(String dst) {
        if (dst == null || dst.isBlank()) {
            return CallType.UNKNOWN;
        }
        String d = dst.replaceAll("[^0-9]", "");

        return switch (d.length()) {
            case 8  -> isFixedSubscriber(d, 0)   ? CallType.FIXED_LOCAL            : CallType.UNKNOWN;
            case 9  -> isMobileSubscriber(d, 0)  ? CallType.MOBILE_LOCAL           : CallType.UNKNOWN;
            case 10 -> isFixedSubscriber(d, 2)   ? CallType.FIXED_LONG_DISTANCE    : CallType.UNKNOWN;
            case 11 -> classify11(d);
            case 12 -> d.charAt(0) == '0' && isMobileSubscriber(d, 3) ? CallType.MOBILE_LONG_DISTANCE : CallType.UNKNOWN;
            case 13 -> d.charAt(0) == '0' && isFixedSubscriber(d, 5)  ? CallType.FIXED_LONG_DISTANCE  : CallType.UNKNOWN;
            case 14 -> d.charAt(0) == '0' && isMobileSubscriber(d, 5) ? CallType.MOBILE_LONG_DISTANCE : CallType.UNKNOWN;
            default -> CallType.UNKNOWN;
        };
    }

    // 11 digits: either 0+DDD(2)+fixed(8) or DDD(2)+mobile(9)
    private CallType classify11(String d) {
        if (d.charAt(0) == '0') {
            return isFixedSubscriber(d, 3) ? CallType.FIXED_LONG_DISTANCE : CallType.UNKNOWN;
        }
        return isMobileSubscriber(d, 2) ? CallType.MOBILE_LONG_DISTANCE : CallType.UNKNOWN;
    }

    // Fixed subscriber: 8 digits starting at offset, first digit in [2-8]
    private boolean isFixedSubscriber(String d, int offset) {
        char first = d.charAt(offset);
        return first >= '2' && first <= '8';
    }

    // Mobile subscriber: 9 digits starting at offset, first digit == '9'
    private boolean isMobileSubscriber(String d, int offset) {
        return d.charAt(offset) == '9';
    }
}
