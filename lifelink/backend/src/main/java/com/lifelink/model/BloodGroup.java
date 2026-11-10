package com.lifelink.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The eight ABO/Rh blood groups. JSON uses the human label ("A+", "O-"),
 * the database stores the enum name.
 */
public enum BloodGroup {
    A_POS("A+"), A_NEG("A-"),
    B_POS("B+"), B_NEG("B-"),
    AB_POS("AB+"), AB_NEG("AB-"),
    O_POS("O+"), O_NEG("O-");

    private final String label;

    BloodGroup(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static BloodGroup fromLabel(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Blood group is required");
        }

        String decoded = raw.stripLeading();
        try {
            decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            // Some callers pass a plain value; leave it as-is.
        }

        decoded = decoded.stripLeading();
        if (decoded.endsWith(" ")) {
            decoded = decoded.stripTrailing() + "+";
        }

        String s = decoded.toUpperCase(Locale.ROOT);

        for (BloodGroup g : values()) {
            if (g.label.equals(s) || g.name().equals(s)) {
                return g;
            }
        }
        throw new IllegalArgumentException("Unknown blood group: " + raw);
    }

    private String abo() {
        return label.substring(0, label.length() - 1);
    }

    private boolean rhPositive() {
        return label.endsWith("+");
    }

    /** True if a donor of this group can safely give red cells to a patient of group {@code recipient}. */
    public boolean canDonateTo(BloodGroup recipient) {
        boolean aboOk = switch (abo()) {
            case "O" -> true;
            case "A" -> recipient.abo().equals("A") || recipient.abo().equals("AB");
            case "B" -> recipient.abo().equals("B") || recipient.abo().equals("AB");
            default -> recipient.abo().equals("AB"); // AB can only give to AB
        };
        boolean rhOk = !rhPositive() || recipient.rhPositive();
        return aboOk && rhOk;
    }

    /** Every group that can donate to this (recipient) group. */
    public static List<BloodGroup> donorsFor(BloodGroup recipient) {
        return Arrays.stream(values()).filter(g -> g.canDonateTo(recipient)).toList();
    }

    /** Every group this (donor) group can donate to. */
    public List<BloodGroup> recipients() {
        return Arrays.stream(values()).filter(this::canDonateTo).toList();
    }
}
