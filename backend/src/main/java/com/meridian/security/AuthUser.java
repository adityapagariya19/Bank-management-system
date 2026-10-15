package com.meridian.security;

import com.meridian.model.Role;

/** Principal stored in the SecurityContext for every authenticated request. */
public record AuthUser(Long id, String email, Role role) {}
