package com.lifelink;

import com.lifelink.model.BloodGroup;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.lifelink.model.BloodGroup.*;
import static org.junit.jupiter.api.Assertions.*;

class BloodGroupTest {

    @Test
    void oNegativeIsUniversalDonor() {
        for (BloodGroup recipient : BloodGroup.values()) {
            assertTrue(O_NEG.canDonateTo(recipient), "O- should donate to " + recipient);
        }
    }

    @Test
    void abPositiveIsUniversalRecipient() {
        assertEquals(8, BloodGroup.donorsFor(AB_POS).size());
    }

    @Test
    void abPositiveDonatesOnlyToAbPositive() {
        assertEquals(List.of(AB_POS), AB_POS.recipients());
    }

    @Test
    void rhPositiveCannotDonateToRhNegative() {
        assertFalse(A_POS.canDonateTo(A_NEG));
        assertFalse(O_POS.canDonateTo(O_NEG));
        assertTrue(A_NEG.canDonateTo(A_POS));
    }

    @Test
    void aboRulesAreRespected() {
        assertTrue(A_POS.canDonateTo(AB_POS));
        assertFalse(A_POS.canDonateTo(B_POS));
        assertFalse(B_NEG.canDonateTo(A_NEG));
        assertFalse(AB_NEG.canDonateTo(O_NEG));
    }

    @Test
    void negativeRecipientsOnlyAcceptNegativeDonors() {
        List<BloodGroup> donors = BloodGroup.donorsFor(B_NEG);
        assertEquals(List.of(B_NEG, O_NEG), donors);
    }

    @Test
    void parsesLabelsIncludingUnencodedPlus() {
        assertEquals(A_POS, BloodGroup.fromLabel("A+"));
        assertEquals(O_NEG, BloodGroup.fromLabel("o-"));
        assertEquals(B_POS, BloodGroup.fromLabel("B ")); // '+' turned into a space by a query string
        assertEquals(AB_NEG, BloodGroup.fromLabel("AB_NEG"));
        assertThrows(IllegalArgumentException.class, () -> BloodGroup.fromLabel("C+"));
    }
}
