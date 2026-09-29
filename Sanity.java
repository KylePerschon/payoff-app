public class Sanity {
    static int checks = 0, fails = 0;
    static void check(String what, boolean ok) {
        checks++; if (!ok) { fails++; System.out.println("FAIL " + what); }
        else System.out.println("ok   " + what);
    }
    public static void main(String[] a) {
        // APR accepted either as percent or fraction, same result
        CreditCard pct  = new CreditCard("pct",  1000.00, 18.24);
        CreditCard frac = new CreditCard("frac", 1000.00, 0.1824);
        check("APR percent and fraction agree", Math.abs(pct.getApr() - frac.getApr()) < 1e-9);

        // minimum payment: 2% floored at $35, capped at balance
        check("2% used when above floor", new CreditCard("x", 5000.0, 0).getMinimumPayment() == 100.00);
        check("$35 floor applied",        new CreditCard("x", 1000.0, 0).getMinimumPayment() == 35.00);
        check("never exceeds balance",    new CreditCard("x",   12.34, 0).getMinimumPayment() == 12.34);
        check("zero balance owes zero",   new CreditCard("x",    0.0,  0).getMinimumPayment() == 0.00);

        // interest compounds: a month must exceed simple interest
        CreditCard c = new CreditCard("c", 10000.00, 24.00);
        double simple = 10000.00 * c.getDailyApr() * 31;
        double actual = c.interestForMonth(1, 2022);
        check("compounds above simple interest", actual > simple);
        check("0% APR accrues nothing", new CreditCard("z", 1000.0, 0.0).interestForMonth(1, 2022) == 0.0);

        // interestForMonth is a query and must not mutate
        double before = c.getBalance();
        c.interestForMonth(1, 2022);
        check("interestForMonth does not mutate", c.getBalance() == before);

        // leap year has a longer February
        CreditCard l = new CreditCard("l", 10000.00, 24.00);
        check("Feb 2024 > Feb 2023", l.interestForMonth(2, 2024) > l.interestForMonth(2, 2023));

        // payments cannot overpay or go negative
        CreditCard p = new CreditCard("p", 100.00, 0.0);
        check("overpay trimmed to balance", p.pay(500.00) == 100.00);
        check("balance lands exactly on 0", p.getBalance() == 0.0);
        check("paid-off card accepts nothing", p.pay(50.0) == 0.0);
        check("negative payment ignored", new CreditCard("n", 100.0, 0).pay(-10.0) == 0.0);

        System.out.printf("%n%d checks, %d failures%n", checks, fails);
        if (fails > 0) System.exit(1);
    }
}
