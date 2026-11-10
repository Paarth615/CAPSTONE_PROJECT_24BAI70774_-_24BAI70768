package com.lifelink.config;

import com.lifelink.model.*;
import com.lifelink.repository.BloodRequestRepository;
import com.lifelink.repository.DonationResponseRepository;
import com.lifelink.repository.UserRepository;
import com.lifelink.service.MatchingService;
import com.lifelink.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Loads a believable demo dataset into an EMPTY database so evaluators have something to explore.
 *
 *   admin@lifelink.com     / Admin@123
 *   donor1..donor28@lifelink.com / Password@123
 *   hospital1..hospital3@lifelink.com (requesters) / Password@123
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final Map<String, double[]> CITIES = Map.ofEntries(
            Map.entry("Lucknow", new double[]{26.8467, 80.9462}),
            Map.entry("Kanpur", new double[]{26.4499, 80.3319}),
            Map.entry("Delhi", new double[]{28.6139, 77.2090}),
            Map.entry("Noida", new double[]{28.5355, 77.3910}),
            Map.entry("Mumbai", new double[]{19.0760, 72.8777}),
            Map.entry("Pune", new double[]{18.5204, 73.8567}),
            Map.entry("Bengaluru", new double[]{12.9716, 77.5946}),
            Map.entry("Hyderabad", new double[]{17.3850, 78.4867}),
            Map.entry("Chennai", new double[]{13.0827, 80.2707}),
            Map.entry("Kolkata", new double[]{22.5726, 88.3639}),
            Map.entry("Varanasi", new double[]{25.3176, 82.9739}),
            Map.entry("Jaipur", new double[]{26.9124, 75.7873}));

    private static final String[] DONOR_NAMES = {
            "Aarav Mishra", "Ananya Srivastava", "Vihaan Tiwari", "Diya Pandey", "Arjun Yadav", "Ishita Gupta",
            "Kabir Saxena", "Meera Agarwal", "Rohan Verma", "Saanvi Kapoor", "Aditya Rathore", "Kritika Singh",
            "Yash Chauhan", "Tanvi Joshi", "Harsh Vardhan", "Pooja Nair", "Rahul Menon", "Sneha Iyer",
            "Nikhil Reddy", "Aisha Khan", "Siddharth Bose", "Riya Das", "Manish Patel", "Neha Shah",
            "Aman Sharma", "Priya Chatterjee", "Deepak Kumar", "Farah Siddiqui"};

    private static final String[] DONOR_CITIES = {
            "Lucknow", "Lucknow", "Lucknow", "Lucknow", "Lucknow", "Lucknow", "Lucknow", "Lucknow",
            "Kanpur", "Kanpur", "Kanpur", "Delhi", "Delhi", "Noida", "Delhi", "Mumbai", "Mumbai", "Pune",
            "Bengaluru", "Bengaluru", "Kolkata", "Kolkata", "Jaipur", "Varanasi", "Hyderabad", "Chennai",
            "Varanasi", "Mumbai"};

    private static final BloodGroup[] GROUPS = {
            BloodGroup.O_POS, BloodGroup.B_POS, BloodGroup.A_POS, BloodGroup.O_NEG, BloodGroup.AB_POS,
            BloodGroup.B_POS, BloodGroup.A_NEG, BloodGroup.O_POS, BloodGroup.B_NEG, BloodGroup.A_POS,
            BloodGroup.AB_NEG, BloodGroup.O_POS, BloodGroup.B_POS, BloodGroup.A_POS};

    private final UserRepository users;
    private final BloodRequestRepository requests;
    private final DonationResponseRepository responses;
    private final MatchingService matching;
    private final NotificationService notifications;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, BloodRequestRepository requests, DonationResponseRepository responses,
                      MatchingService matching, NotificationService notifications, PasswordEncoder encoder) {
        this.users = users;
        this.requests = requests;
        this.responses = responses;
        this.matching = matching;
        this.notifications = notifications;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            log.info("Database already has data - skipping demo seed");
            return;
        }
        log.info("Seeding demo data...");
        Random rnd = new Random(42);
        LocalDate today = LocalDate.now();

        // Admin
        User admin = baseUser("LifeLink Admin", "admin@lifelink.com", "Admin@123", Role.ADMIN, "Lucknow", "9000000000");
        users.save(admin);

        // Donors
        for (int i = 0; i < DONOR_NAMES.length; i++) {
            String city = DONOR_CITIES[i];
            User d = baseUser(DONOR_NAMES[i], "donor" + (i + 1) + "@lifelink.com", "Password@123", Role.DONOR,
                    city, "98765" + String.format("%05d", i + 1));
            d.setBloodGroup(GROUPS[i % GROUPS.length]);
            d.setDateOfBirth(LocalDate.of(1984 + rnd.nextInt(18), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)));
            d.setWeightKg(55.0 + rnd.nextInt(35));
            // Variety so eligibility is visible: never donated / rested / still in cool-down
            int mode = i % 4;
            if (mode == 1) {
                d.setLastDonationDate(today.minusDays(120 + rnd.nextInt(240)));
            } else if (mode == 2) {
                d.setLastDonationDate(today.minusDays(20 + rnd.nextInt(40)));
            } else if (mode == 3) {
                d.setLastDonationDate(today.minusDays(100 + rnd.nextInt(60)));
            }
            if (i == 6) {
                d.setAvailable(false); // one donor currently unavailable
            }
            jitter(d, rnd);
            users.save(d);
        }

        // Requesters (hospitals / families)
        User h1 = requester("King George's Medical University Blood Centre", "hospital1@lifelink.com", "Lucknow", "9415000001");
        User h2 = requester("Regency Hospital Desk", "hospital2@lifelink.com", "Kanpur", "9415000002");
        User h3 = requester("Sunita Rao (family)", "hospital3@lifelink.com", "Bengaluru", "9415000003");
        users.saveAll(List.of(h1, h2, h3));

        // Requests
        List<BloodRequest> seeded = List.of(
                request(h1, "Rohan Verma", BloodGroup.B_POS, 2, "King George's Medical University", "Lucknow",
                        Urgency.CRITICAL, today.plusDays(1), "9415000001", "Post-surgery, needs units today or tomorrow."),
                request(h1, "Aarav Singh", BloodGroup.A_POS, 3, "SGPGI", "Lucknow",
                        Urgency.URGENT, today.plusDays(2), "9415000001", "Planned cardiac procedure."),
                request(h1, "Meera Joshi", BloodGroup.O_NEG, 1, "Balrampur Hospital", "Lucknow",
                        Urgency.URGENT, today.plusDays(3), "9415000001", "Rare group, any O- donor can help."),
                request(h2, "Kabir Das", BloodGroup.O_POS, 2, "Regency Hospital", "Kanpur",
                        Urgency.NORMAL, today.plusDays(5), "9415000002", null),
                request(h3, "Nisha Rao", BloodGroup.B_NEG, 1, "Manipal Hospital", "Bengaluru",
                        Urgency.CRITICAL, today.plusDays(1), "9415000003", "Accident case, urgent."),
                request(h3, "Sana Khan", BloodGroup.AB_NEG, 1, "Fortis Hospital", "Bengaluru",
                        Urgency.NORMAL, today.plusDays(7), "9415000003", "Scheduled transfusion."));
        requests.saveAll(seeded);

        // Alert the best matches, like the live app does when a request is created
        for (BloodRequest r : seeded) {
            matching.findMatches(r, 8).forEach(m ->
                    users.findById(m.donorId()).ifPresent(d -> notifications.notify(d,
                            r.getUrgency() + ": " + r.getUnitsRequired() + " unit(s) of " + r.getBloodGroup().getLabel()
                                    + " needed at " + r.getHospital() + ", " + r.getCity() + ". You are a match.",
                            "/requests/" + r.getId())));
        }

        // One pending and one accepted response so the workflow is visible straight away
        BloodRequest first = seeded.get(0);
        List<MatchingPick> picks = matching.findMatches(first, 2).stream()
                .map(m -> new MatchingPick(m.donorId())).toList();
        if (picks.size() >= 1) {
            users.findById(picks.get(0).donorId()).ifPresent(d -> saveResponse(first, d, ResponseStatus.ACCEPTED,
                    "I can reach the hospital this evening."));
        }
        if (picks.size() >= 2) {
            users.findById(picks.get(1).donorId()).ifPresent(d -> saveResponse(first, d, ResponseStatus.PENDING,
                    "Available tomorrow morning."));
        }
        log.info("Demo data ready: {} users, {} requests", users.count(), requests.count());
    }

    private record MatchingPick(Long donorId) {
    }

    private void saveResponse(BloodRequest req, User donor, ResponseStatus status, String message) {
        DonationResponse r = new DonationResponse();
        r.setRequest(req);
        r.setDonor(donor);
        r.setStatus(status);
        r.setMessage(message);
        responses.save(r);
    }

    private User baseUser(String name, String email, String rawPassword, Role role, String city, String phone) {
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role);
        u.setCity(city);
        u.setPhone(phone);
        double[] c = CITIES.get(city);
        if (c != null) {
            u.setLatitude(c[0]);
            u.setLongitude(c[1]);
        }
        return u;
    }

    private User requester(String name, String email, String city, String phone) {
        return baseUser(name, email, "Password@123", Role.REQUESTER, city, phone);
    }

    /** Spreads donors a few km around their city centre so distances differ. */
    private void jitter(User u, Random rnd) {
        if (u.getLatitude() != null) {
            u.setLatitude(u.getLatitude() + (rnd.nextDouble() - 0.5) * 0.12);
            u.setLongitude(u.getLongitude() + (rnd.nextDouble() - 0.5) * 0.12);
        }
    }

    private BloodRequest request(User requester, String patient, BloodGroup group, int units, String hospital,
                                 String city, Urgency urgency, LocalDate by, String phone, String notes) {
        BloodRequest r = new BloodRequest();
        r.setRequester(requester);
        r.setPatientName(patient);
        r.setBloodGroup(group);
        r.setUnitsRequired(units);
        r.setHospital(hospital);
        r.setCity(city);
        double[] c = CITIES.get(city);
        if (c != null) {
            r.setLatitude(c[0]);
            r.setLongitude(c[1]);
        }
        r.setUrgency(urgency);
        r.setRequiredBy(by);
        r.setContactPhone(phone);
        r.setNotes(notes);
        return r;
    }
}
