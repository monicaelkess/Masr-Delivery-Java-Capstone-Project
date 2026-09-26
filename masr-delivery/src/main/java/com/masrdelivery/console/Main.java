package com.masrdelivery.console;

import com.masrdelivery.config.PlatformConfig;
import com.masrdelivery.exception.PlatformException;
import com.masrdelivery.service.MutableClock;
import com.masrdelivery.service.Platform;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class Main {
    public static void main(String[] args) throws PlatformException {
        PlatformConfig config = PlatformConfig.getInstance();
        Platform platform = new Platform(config, MutableClock.system(), config.auditLogFile());
        DataSeeder.seed(platform);
        new ConsoleApp(platform, new InputReader(new Scanner(System.in, StandardCharsets.UTF_8), System.out)).run();
    }
}
