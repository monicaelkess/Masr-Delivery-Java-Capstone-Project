package com.masrdelivery.event;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AuditLog implements OrderStatusListener {
    private final List<String> entries = new CopyOnWriteArrayList<>();
    private final Path file;
    private volatile boolean fileBroken;


    public AuditLog(Path file) { this.file = file; }

    @Override public void onStatusChanged(OrderStatusChangedEvent e) {
        String line = e.at() + " " + e.order().id() + " " + e.from().map(Enum::name).orElse("NEW") + " -> " + e.to()
                + e.order().riderId().map(r -> " rider=" + r).orElse("");
        entries.add(line);
        if (file != null && !fileBroken) write(line);
    }

    private synchronized void write(String line) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Files.writeString(file, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            fileBroken = true;
            entries.add("AUDIT FILE UNAVAILABLE (" + file + "): " + ex.getMessage());
        }
    }

    public List<String> entries() { return List.copyOf(entries); }
}
