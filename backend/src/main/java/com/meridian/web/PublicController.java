package com.meridian.web;

import com.meridian.dto.Dtos.*;
import com.meridian.model.OutboxMessage;
import com.meridian.repo.OutboxRepository;
import com.meridian.service.BankException;
import com.meridian.service.CustomerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PublicController {
    private final CustomerService customers;
    private final OutboxRepository outbox;
    private final boolean inboxEnabled;

    public PublicController(CustomerService customers, OutboxRepository outbox,
                            @Value("${app.dev-inbox.enabled:false}") boolean inboxEnabled) {
        this.customers = customers;
        this.outbox = outbox;
        this.inboxEnabled = inboxEnabled;
    }

    @GetMapping("/public/banks")
    public List<BankDto> banks() { return customers.banks(); }

    @GetMapping("/public/config")
    public java.util.Map<String, Object> config() { return java.util.Map.of("simulatedInbox", inboxEnabled); }

    /** Simulated SMS / e-mail gateway. Shows messages sent to a recipient in the last 30 minutes. */
    @GetMapping("/dev/outbox")
    public List<OutboxDto> outbox(@RequestParam String recipient) {
        if (!inboxEnabled) throw new BankException("Simulated inbox is disabled", HttpStatus.NOT_FOUND);
        String r = recipient.trim().toLowerCase();
        if (!r.contains("@")) {
            String d = r.replaceAll("\\D", "");
            r = d.length() > 10 ? d.substring(d.length() - 10) : d;
        }
        // an e-mail identifier also matches the SMS sent to the same user's phone only if the phone is typed separately
        List<String> keys = List.of(r);
        return outbox.findTop20ByRecipientInAndCreatedAtAfterOrderByCreatedAtDesc(keys, LocalDateTime.now().minusMinutes(30)).stream()
                .map((OutboxMessage m) -> new OutboxDto(m.getId(), m.getChannel(), m.getRecipient(), m.getSubject(), m.getBody(), m.getCreatedAt())).toList();
    }
}
