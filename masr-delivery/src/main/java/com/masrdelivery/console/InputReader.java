package com.masrdelivery.console;

import com.masrdelivery.money.Money;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Function;

public final class InputReader {

       public static final class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
        EndOfInput() { super("end of input"); }
    }

    private final Scanner in;
    private final PrintStream out;

    public InputReader(Scanner in, PrintStream out) { this.in = in; this.out = out; }

    public PrintStream out() { return out; }

    public String line(String prompt) {
        out.print(prompt);
        out.flush();
        if (!in.hasNextLine()) throw new EndOfInput();
        return in.nextLine().trim();
    }

    public String text(String prompt) {
        while (true) {
            String s = line(prompt);
            if (!s.isEmpty()) return s;
            out.println("  Please enter a value.");
        }
    }

    public Optional<String> optionalText(String prompt) {
        String s = line(prompt);
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    public int choice(String prompt, int min, int max) {
        while (true) {
            String s = line(prompt);
            try {
                int n = Integer.parseInt(s);
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException ignored) { }
            out.println("  Invalid option. Enter a number from " + min + " to " + max + ".");
        }
    }

    public BigDecimal positiveDecimal(String prompt) {
        while (true) {
            String s = line(prompt);
            try {
                BigDecimal d = new BigDecimal(s);
                if (d.signum() > 0) return d;
                out.println("  Must be greater than zero.");
            } catch (NumberFormatException e) {
                out.println("  Not a number: " + s);
            }
        }
    }

    public BigDecimal nonNegativeDecimal(String prompt) {
        while (true) {
            String s = line(prompt);
            try {
                BigDecimal d = new BigDecimal(s);
                if (d.signum() >= 0) return d;
                out.println("  Must not be negative.");
            } catch (NumberFormatException e) {
                out.println("  Not a number: " + s);
            }
        }
    }

    public Money money(String prompt) { return Money.of(positiveDecimal(prompt)); }

    public Optional<Money> optionalMoney(String prompt) {
        while (true) {
            String s = line(prompt);
            if (s.isEmpty()) return Optional.empty();
            try {
                BigDecimal d = new BigDecimal(s);
                if (d.signum() > 0) return Optional.of(Money.of(d));
                out.println("  Must be greater than zero.");
            } catch (NumberFormatException e) {
                out.println("  Not a number: " + s);
            }
        }
    }

    public double rating(String prompt) {
        while (true) {
            String s = line(prompt);
            try {
                double r = Double.parseDouble(s);
                if (r >= 0.0 && r <= 5.0) return r;
            } catch (NumberFormatException ignored) { }
            out.println("  Rating must be a number between 0.0 and 5.0.");
        }
    }

    public boolean yesNo(String prompt) {
        while (true) {
            String s = line(prompt + " (y/n): ").toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
            out.println("  Please answer y or n.");
        }
    }

    public LocalDate date(String prompt) {
        while (true) {
            String s = line(prompt + " (yyyy-mm-dd): ");
            try { return LocalDate.parse(s); }
            catch (DateTimeParseException e) { out.println("  Not a valid date: " + s); }
        }
    }

    public YearMonth yearMonth(String prompt) {
        while (true) {
            String s = line(prompt + " (yyyy-mm): ");
            try { return YearMonth.parse(s); }
            catch (DateTimeParseException e) { out.println("  Not a valid month: " + s); }
        }
    }


    public <T> Optional<T> pick(String title, List<T> options, Function<T, String> label) {
        if (options.isEmpty()) {
            out.println("  (nothing to choose from)");
            return Optional.empty();
        }
        out.println(title);
        for (int i = 0; i < options.size(); i++) out.printf("  %2d. %s%n", i + 1, label.apply(options.get(i)));
        out.println("   0. Back");
        int n = choice("Choose: ", 0, options.size());
        return n == 0 ? Optional.empty() : Optional.of(options.get(n - 1));
    }
}
