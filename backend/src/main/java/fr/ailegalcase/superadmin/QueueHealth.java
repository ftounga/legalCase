package fr.ailegalcase.superadmin;

public record QueueHealth(
        String name,
        long messagesReady,
        long messagesUnacknowledged,
        long consumers,
        boolean available) {}
