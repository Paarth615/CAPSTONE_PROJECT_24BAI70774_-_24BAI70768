package com.lifelink.service;

import com.lifelink.exception.ApiException;
import com.lifelink.model.Role;
import com.lifelink.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * Basic donor safety rules (modelled on common Indian whole-blood guidelines):
 * age 18-65, weight at least 45 kg, and a cool-down between donations (default 90 days).
 */
@Service
public class EligibilityService {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 65;
    public static final double MIN_WEIGHT_KG = 45.0;

    private final int cooldownDays;

    public EligibilityService(@Value("${app.matching.cooldown-days:90}") int cooldownDays) {
        this.cooldownDays = cooldownDays;
    }

    public int getCooldownDays() {
        return cooldownDays;
    }

    public LocalDate nextEligibleDate(User u) {
        return u.getLastDonationDate() == null ? null : u.getLastDonationDate().plusDays(cooldownDays);
    }

    public boolean cooledDown(User u) {
        LocalDate next = nextEligibleDate(u);
        return next == null || !next.isAfter(LocalDate.now());
    }

    public Integer age(User u) {
        return u.getDateOfBirth() == null ? null : Period.between(u.getDateOfBirth(), LocalDate.now()).getYears();
    }

    /** Throws 400 unless the date of birth gives an age inside the donor range. */
    public void requireDonorAge(LocalDate dob) {
        if (dob == null) {
            throw ApiException.badRequest("Date of birth is required for donors");
        }
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (dob.isAfter(LocalDate.now()) || age < MIN_AGE || age > MAX_AGE) {
            throw ApiException.badRequest("Donors must be between " + MIN_AGE + " and " + MAX_AGE + " years old");
        }
    }

    /** Throws 400 when a weight was given but is not a believable value. */
    public void requireSaneWeight(Double weightKg) {
        if (weightKg != null && (weightKg < 30 || weightKg > 250)) {
            throw ApiException.badRequest("Enter a weight between 30 and 250 kg");
        }
    }

    /** Returns null when the user can donate right now, otherwise a plain-language reason. */
    public String ineligibleReason(User u) {
        if (u.getRole() != Role.DONOR) {
            return "Only donor accounts can donate";
        }
        if (u.getBloodGroup() == null) {
            return "Add your blood group to your profile";
        }
        Integer age = age(u);
        if (age == null) {
            return "Add your date of birth to your profile";
        }
        if (age < MIN_AGE || age > MAX_AGE) {
            return "Donors must be between " + MIN_AGE + " and " + MAX_AGE + " years old";
        }
        if (u.getWeightKg() != null && u.getWeightKg() < MIN_WEIGHT_KG) {
            return "Donors must weigh at least " + (int) MIN_WEIGHT_KG + " kg";
        }
        if (!cooledDown(u)) {
            return "You can donate again on " + nextEligibleDate(u);
        }
        if (!u.isAvailable()) {
            return "You have marked yourself unavailable";
        }
        return null;
    }

    public boolean isEligible(User u) {
        return ineligibleReason(u) == null;
    }
}
