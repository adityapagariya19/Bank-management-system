package com.meridian.service;

import com.meridian.model.AuditLog;
import com.meridian.repo.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    /** Joins the caller's transaction (rolled back together with the business operation). */
    @Transactional
    public void log(Long actorId, String actorEmail, String action, String entity, String entityId, String details, boolean success) {
        repo.save(build(actorId, actorEmail, action, entity, entityId, details, success));
    }

    /** Always committed, even if the caller's transaction rolls back (used for failures / security events). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIndependent(Long actorId, String actorEmail, String action, String entity, String entityId, String details, boolean success) {
        repo.save(build(actorId, actorEmail, action, entity, entityId, details, success));
    }

    private AuditLog build(Long actorId, String actorEmail, String action, String entity, String entityId, String details, boolean success) {
        AuditLog a = new AuditLog();
        a.setActorId(actorId);
        a.setActorEmail(actorEmail);
        a.setAction(action);
        a.setEntity(entity);
        a.setEntityId(entityId);
        a.setDetails(details == null ? null : details.length() > 500 ? details.substring(0, 500) : details);
        a.setSuccess(success);
        a.setIpAddress(clientIp());
        return a;
    }

    private static String clientIp() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes sra) {
                HttpServletRequest r = sra.getRequest();
                String fwd = r.getHeader("X-Forwarded-For");
                return fwd != null && !fwd.isBlank() ? fwd.split(",")[0].trim() : r.getRemoteAddr();
            }
        } catch (Exception ignored) {
            // no request bound to this thread (e.g. startup seeding)
        }
        return null;
    }
}
