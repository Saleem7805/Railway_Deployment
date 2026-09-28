package com.proitbridge.lms.config;

import com.proitbridge.lms.domain.Feature;
import com.proitbridge.lms.domain.FormSection;
import com.proitbridge.lms.domain.User;
import com.proitbridge.lms.repo.FeatureRepository;
import com.proitbridge.lms.repo.FormSectionRepository;
import com.proitbridge.lms.repo.UserRepository;
import com.proitbridge.lms.service.CredentialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** 
 * Author: SALEEM
 * 
 * Runs once every time the app starts and fills in what a new database needs:
 *   1. feature toggles  (adds only the missing ones, on every start)
 *   2. starter form     (only if there are no form sections at all)
 *   3. first super admin (only if there are no users at all)
 * It never changes or brings back anything that already exists / was deleted.
 */
@Component
public class BootstrapSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapSeeder.class);

    // ---------- default data ----------

    /** One row of the feature table: key, label, on for premium?, on for batch? */
    private record FeatureDefault(String key, String label, boolean forPremium, boolean forBatch) {}

    private static final List<FeatureDefault> DEFAULT_FEATURES = List.of(
        new FeatureDefault("biweekly_call",      "Biweekly progress call",      true,  false),
        new FeatureDefault("live_sessions",      "Live sessions",               true,  true),
        new FeatureDefault("industry_sessions",  "Industry expert sessions",    true,  true),
        new FeatureDefault("doubt_clearing",     "Doubt clearing",              true,  true),
        new FeatureDefault("one_to_one_booking", "One to one slot booking",     true,  false),
        new FeatureDefault("group_doubt",        "Group doubt clearing",        false, true),
        new FeatureDefault("project_sessions",   "Project sessions",            true,  true),
        new FeatureDefault("jobs_referrals",     "Job openings and referrals",  true,  true),
        new FeatureDefault("case_studies",       "Case studies",                true,  true),
        new FeatureDefault("ai_teach_back",      "AI teach back test",          true,  true),
        new FeatureDefault("cohort_pace",        "Cohort pace visibility",      false, true),
        new FeatureDefault("batch_leaderboard",  "Batch leaderboard",           false, true),
        new FeatureDefault("induction_gate",     "Fixed Tuesday induction gate", false, true),
        new FeatureDefault("top10_guidance",     "Top 10 learner guidance",     true,  true)
    );

    /** One row of the starter form: key, title, hint. */
    private record SectionDefault(String key, String label, String note) {}

    private static final List<SectionDefault> DEFAULT_SECTIONS = List.of(
        new SectionDefault("basic",        "About you",               "Name, contact and where you are based."),
        new SectionDefault("education",    "Education",               "What you studied and where."),
        new SectionDefault("professional", "Work",                    "Current or most recent role, and how long."),
        new SectionDefault("technical",    "What you already know",   "Tools and languages, honestly rated."),
        new SectionDefault("projects",     "Anything you have built", "Even small things count."),
        new SectionDefault("resume",       "Resume",                  "Upload the latest one."),
        new SectionDefault("intent",       "What you want from this", "The role you are aiming at."),
        new SectionDefault("links",        "Links",                   "Portfolio, repository, professional profile.")
    );

    // ---------- Repo ----------

    private final FeatureRepository featureRepo;
    private final FormSectionRepository sectionRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Value("${lms.bootstrap.email}")    private String adminEmail;
    @Value("${lms.bootstrap.password}") private String adminPassword;
    @Value("${lms.bootstrap.name}")     private String adminName;

    public BootstrapSeeder(FeatureRepository featureRepo, FormSectionRepository sectionRepo,
                           UserRepository userRepo, PasswordEncoder encoder) {
        this.featureRepo = featureRepo;
        this.sectionRepo = sectionRepo;
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    // ---------- runs at startup ----------

    @Override
    public void run(ApplicationArguments args) {
        addMissingFeatures();
        addStarterFormIfEmpty();
        createSuperAdminIfNoUsers();
    }

    /** 1. Every start: add any feature that is not in the database yet. */
    private void addMissingFeatures() {
        for (FeatureDefault d : DEFAULT_FEATURES) {
            if (featureRepo.findByKey(d.key()).isPresent()) {
                continue; // already there, leave the admin's setting alone
            }
            Feature f = new Feature();
            f.setKey(d.key());
            f.setLabel(d.label());
            f.setForPremium(d.forPremium());
            f.setForBatch(d.forBatch());
            featureRepo.save(f);
            log.info("Added the feature toggle {}", d.key());
        }
    }

    /** 2. Only on an empty database: create the 8 starter form sections. */
    private void addStarterFormIfEmpty() {
        if (sectionRepo.count() > 0) {
            return; // sections exist, so a deleted one must not come back
        }
        for (int i = 0; i < DEFAULT_SECTIONS.size(); i++) {
            SectionDefault d = DEFAULT_SECTIONS.get(i);
            FormSection s = new FormSection();
            s.setKey(d.key());
            s.setLabel(d.label());
            s.setNote(d.note());
            s.setPosition(i);
            sectionRepo.save(s);
        }
        log.info("Created {} starting form sections. All of them are editable.", DEFAULT_SECTIONS.size());
    }

    /** 3. Only when there are no users: create the one super admin. */
    private void createSuperAdminIfNoUsers() {
        if (userRepo.count() > 0) {
            return; // users exist, never recreate or reset anyone
        }
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("No users and no BOOTSTRAP_EMAIL set. Nobody can sign in. "
                    + "Set BOOTSTRAP_EMAIL and BOOTSTRAP_PASSWORD and restart.");
            return;
        }

        String name = (adminName == null || adminName.isBlank()) ? "Super admin" : adminName;

        User u = new User();
        u.setEmail(adminEmail.trim().toLowerCase());
        u.setFullName(name);
        u.setLoginId(CredentialService.slug(name));
        u.setRole(User.Role.SUPER_ADMIN);
        u.setPasswordHash(encoder.encode(adminPassword));
        u.setActive(true);
        u.setMustChangePassword(true); // the env password is a one-time key
        userRepo.save(u);

        log.info("First run: created the super admin {} (login id {}). "
                + "You will be asked to set a new password when you sign in.",
                u.getEmail(), u.getLoginId());
    }
}