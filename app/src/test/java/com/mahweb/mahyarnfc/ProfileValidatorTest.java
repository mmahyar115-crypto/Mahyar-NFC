package com.mahweb.mahyarnfc;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProfileValidatorTest {
    @Test public void blankNameIsRejected() {
        assertNotNull(ProfileValidator.validateName("   "));
    }

    @Test public void normalNameIsAccepted() {
        assertNull(ProfileValidator.validateName("مهیار رضایی"));
    }

    @Test public void blankPhoneIsRejected() {
        assertNotNull(ProfileValidator.validatePhone(""));
    }

    @Test public void internationalPhoneIsAccepted() {
        assertNull(ProfileValidator.validatePhone("+98 912 123 4567"));
    }

    @Test public void malformedEmailIsRejected() {
        assertNotNull(ProfileValidator.validateEmail("bad@email"));
    }

    @Test public void blankEmailIsOptional() {
        assertNull(ProfileValidator.validateEmail(""));
    }

    @Test public void websiteGetsHttpsScheme() {
        assertEquals("https://example.com", ProfileValidator.normalizeWebsite("example.com"));
    }

    @Test public void readyProfileNeedsNameAndPhone() {
        Profile p = new Profile();
        p.name = "مهیار";
        p.phone = "+989121234567";
        assertTrue(ProfileValidator.isProfileReady(p));
        p.phone = "";
        assertFalse(ProfileValidator.isProfileReady(p));
    }
}
