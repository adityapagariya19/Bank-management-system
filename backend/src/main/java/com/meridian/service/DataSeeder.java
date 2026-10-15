package com.meridian.service;

import com.meridian.model.*;
import com.meridian.repo.BankRepository;
import com.meridian.repo.BranchRepository;
import com.meridian.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds REFERENCE data only: the participating banks with their branches and the four staff logins.
 * There are no fake customers, balances or transactions - those are created through the real flows.
 */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final String[][] BANKS = {
            // code, name, short name, brand colour, headquarters
            {"SBIN", "State Bank of India", "SBI", "#1e6fb5", "Mumbai"},
            {"HDFC", "HDFC Bank", "HDFC", "#0b3d7a", "Mumbai"},
            {"ICIC", "ICICI Bank", "ICICI", "#b4531f", "Mumbai"},
            {"UTIB", "Axis Bank", "Axis", "#8e1f4a", "Mumbai"},
            {"PUNB", "Punjab National Bank", "PNB", "#a3262a", "New Delhi"},
            {"KKBK", "Kotak Mahindra Bank", "Kotak", "#0a5c7a", "Mumbai"},
    };
    private static final String[][] CITIES = {
            {"Mumbai", "Maharashtra"}, {"New Delhi", "Delhi"}, {"Bengaluru", "Karnataka"}, {"Hyderabad", "Telangana"}, {"Pune", "Maharashtra"},
    };
    private static final String STAFF_PASSWORD = "Meridian@123";

    private final UserRepository users;
    private final BankRepository banks;
    private final BranchRepository branches;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, BankRepository banks, BranchRepository branches, PasswordEncoder encoder) {
        this.users = users;
        this.banks = banks;
        this.branches = branches;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (banks.count() == 0) {
            for (String[] b : BANKS) {
                Bank bank = new Bank();
                bank.setCode(b[0]);
                bank.setName(b[1]);
                bank.setShortName(b[2]);
                bank.setColor(b[3]);
                bank.setHeadquarters(b[4]);
                banks.save(bank);
                int n = 1;
                for (String[] c : CITIES) {
                    Branch br = new Branch();
                    br.setBank(bank);
                    br.setName(c[0] + " Main Branch");
                    br.setCity(c[0]);
                    br.setState(c[1]);
                    // IFSC format: 4 letters + 0 + 6 chars. "MRD" marks these as simulated codes (not real branch codes).
                    br.setIfsc(b[0] + "0MRD" + String.format("%03d", n++));
                    branches.save(br);
                }
            }
        }
        if (users.count() == 0) {
            staff("Meridian Administrator", "admin@meridian.bank", "9000000001", Role.ADMIN);
            staff("Kavita Rao", "manager@meridian.bank", "9000000002", Role.MANAGER);
            staff("Rahul Nair", "teller@meridian.bank", "9000000003", Role.TELLER);
            staff("Neha Kapoor", "auditor@meridian.bank", "9000000004", Role.AUDITOR);
            System.out.println("""

                    ==============================================================
                      MERIDIAN first-run setup complete
                      Staff logins (password for all: Meridian@123)
                        admin@meridian.bank    ADMIN
                        manager@meridian.bank  MANAGER
                        teller@meridian.bank   TELLER
                        auditor@meridian.bank  AUDITOR
                      Customers register themselves at http://localhost:5173
                    ==============================================================
                    """);
        }
    }

    private void staff(String name, String email, String phone, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(STAFF_PASSWORD));
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        users.save(u);
    }
}
