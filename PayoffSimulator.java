/**
 * Debt snowball simulator.
 *
 * Each month: bill every account its interest, pay every minimum, then throw
 * whatever is left of a fixed monthly budget at the SMALLEST remaining balance.
 * Repeat until everything is clear, then report how long it took and what it
 * cost.
 *
 * The snowball orders by balance rather than by interest rate. Paying the
 * highest rate first (the "avalanche") costs less in interest; clearing the
 * smallest balance first frees a whole minimum payment sooner and gives you a
 * closed account to point at. This models the snowball — swap the comparison in
 * findTarget() for getApr() to model the avalanche instead.
 */
public class PayoffSimulator {

    /** Refuse to run forever if the budget cannot cover the minimums. */
    private static final int MAX_MONTHS = 1200; // 100 years

    public static void main(String[] args) {
        // ---- Example figures. Replace with your own. --------------------
        CreditCard[] cards = {
            new CreditCard("Card A", 4800.00, 18.24),
            new CreditCard("Card B",  840.00, 22.99),
            new CreditCard("Card C",  280.00,  0.00),   // promotional 0%
            new CreditCard("Card D", 1400.00,  0.00),
        };

        double monthlyBudget = 500.00;
        int month = 11;
        int year  = 2021;
        // ----------------------------------------------------------------

        double totalMinimums = 0.0;
        for (CreditCard card : cards) {
            totalMinimums += card.getMinimumPayment();
        }
        if (monthlyBudget < totalMinimums) {
            System.out.printf(
                "Budget of $%.2f does not cover the combined minimums of $%.2f.%n"
                + "The balances would grow every month, so there is nothing to simulate.%n",
                monthlyBudget, totalMinimums);
            return;
        }

        System.out.printf("Starting balances (total $%.2f), budget $%.2f/month%n%n",
                          totalBalance(cards), monthlyBudget);

        int months = 0;
        while (totalBalance(cards) > 0 && months < MAX_MONTHS) {
            months++;

            // 1. Interest accrues on every account still carrying a balance.
            for (CreditCard card : cards) {
                if (!card.isPaidOff()) {
                    card.bill(month, year);
                }
            }

            // 2. Minimums come out first — they are not optional.
            double remaining = monthlyBudget;
            for (CreditCard card : cards) {
                remaining -= card.payMinimum();
            }

            // 3. Everything left goes at the smallest balance. That is the
            //    snowball: one account closes sooner, and its minimum joins
            //    the budget for the next target.
            while (remaining > 0.005) {
                CreditCard target = findTarget(cards);
                if (target == null) {
                    break;                       // all clear
                }
                double applied = target.pay(remaining);
                if (applied <= 0) {
                    break;                       // nothing could be applied
                }
                remaining -= applied;            // roll the overflow onward
            }

            // 4. Advance the calendar. The original incremented the month twice
            //    and never rolled the year, so every month after the first used
            //    the wrong number of days.
            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }

        if (totalBalance(cards) > 0) {
            System.out.printf("Stopped after %d months with $%.2f outstanding.%n",
                              months, totalBalance(cards));
            return;
        }

        report(cards, months);
    }

    /** The account with the smallest non-zero balance, or null if all are clear. */
    private static CreditCard findTarget(CreditCard[] cards) {
        CreditCard target = null;
        for (CreditCard card : cards) {
            if (card.isPaidOff()) {
                continue;
            }
            // Compared with < on primitives. The original compared boxed Doubles
            // with ==, which tests identity rather than value.
            if (target == null || card.getBalance() < target.getBalance()) {
                target = card;
            }
        }
        return target;
    }

    private static double totalBalance(CreditCard[] cards) {
        double total = 0.0;
        for (CreditCard card : cards) {
            total += card.getBalance();
        }
        return total;
    }

    private static void report(CreditCard[] cards, int months) {
        double paid = 0.0;
        double interest = 0.0;

        System.out.printf("%-10s %10s %10s %8s%n", "Account", "Paid", "Interest", "Payments");
        System.out.println("-".repeat(41));
        for (CreditCard card : cards) {
            System.out.printf("%-10s %10.2f %10.2f %8d%n",
                card.getName(), card.getPaymentsTotal(),
                card.getInterestCharged(), card.getPaymentsMade());
            paid += card.getPaymentsTotal();
            interest += card.getInterestCharged();
        }
        System.out.println("-".repeat(41));
        System.out.printf("%-10s %10.2f %10.2f%n%n", "Total", paid, interest);
        System.out.printf("Debt free in %d months (%d years, %d months).%n",
                          months, months / 12, months % 12);
        System.out.printf("Interest was %.1f%% of everything paid.%n",
                          paid > 0 ? interest / paid * 100.0 : 0.0);
    }
}
