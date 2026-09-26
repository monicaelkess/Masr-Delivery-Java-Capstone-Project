package com.masrdelivery.console;

import com.masrdelivery.exception.PlatformException;
import com.masrdelivery.service.Platform;

import java.io.PrintStream;


public final class ConsoleApp {

    @FunctionalInterface
    interface Action { void run() throws PlatformException; }

    private final Platform platform;
    private final InputReader in;
    private final PrintStream out;

    public ConsoleApp(Platform platform, InputReader in) {
        this.platform = platform;
        this.in = in;
        this.out = in.out();
    }

    public void run() {
        CustomerArea customer = new CustomerArea(platform, in);
        RestaurantArea restaurant = new RestaurantArea(platform, in);
        RiderArea rider = new RiderArea(platform, in);
        AdminArea admin = new AdminArea(platform, in);
        try {
            while (true) {
                out.println();
                out.println("============================================");
                out.println(" MASR DELIVERY - Main Menu");
                out.println("============================================");
                out.println(" 1. Customer");
                out.println(" 2. Restaurant");
                out.println(" 3. Rider");
                out.println(" 4. Admin & Reports");
                out.println(" 0. Exit");
                out.println("============================================");
                switch (in.choice("Choose an option: ", 0, 4)) {
                    case 1 -> customer.enter();
                    case 2 -> restaurant.enter();
                    case 3 -> rider.enter();
                    case 4 -> admin.enter();
                    case 0 -> { out.println("Ma'a el-salama!"); return; }
                    default -> throw new AssertionError();
                }
            }
        } catch (InputReader.EndOfInput e) {
            out.println();
            out.println("Input closed. Goodbye.");
        }
    }

        static void attempt(PrintStream out, Action action) {
        try {
            action.run();
        } catch (PlatformException e) {
            out.println("  ERROR: " + e.getMessage());
        } catch (IllegalArgumentException | IllegalStateException e) {
            out.println("  ERROR: " + e.getMessage());
        }
    }
}
