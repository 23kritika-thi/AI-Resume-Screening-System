package com.resumescreening.services;

import com.resumescreening.models.Recruiter;

/**
 * Holds the authenticated recruiter for the lifetime of the desktop session.
 */
public class RecruiterSession {

    private Recruiter currentRecruiter;

    public void login(Recruiter recruiter) {
        if (recruiter == null) {
            throw new IllegalArgumentException("An authenticated recruiter is required.");
        }
        currentRecruiter = recruiter;
    }

    public Recruiter getCurrentRecruiter() {
        return currentRecruiter;
    }

    public boolean isAuthenticated() {
        return currentRecruiter != null;
    }

    public void logout() {
        currentRecruiter = null;
    }
}
