package com.fitnessaddicted.gym.notification.service;

/** Sends one email. Tests swap in a recording version so no real mail is ever sent. */
public interface EmailSender {

    /** Sends a plain-text message. Any failure must be thrown as a RuntimeException so the caller can retry. */
    void send(String to, String subject, String body);
}