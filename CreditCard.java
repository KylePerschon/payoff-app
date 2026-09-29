import java.time.Year;
import java.time.YearMonth;

/**
 * A revolving credit account: balance, APR, and the minimum payment rule.
 *
 * Interest is modelled the way card issuers actually charge it — a daily rate
 * derived from the annual one, compounded across the real number of days in the
 * billing month.
 */
public class CreditCard {

    /** Minimum payment is this share of the balance... */
    private static final double MINIMUM_PAYMENT_RATE = 0.02;
    /** ...but never less than this, unless the balance itself is smaller. */
    private static final double MINIMUM_PAYMENT_FLOOR = 35.00;

    private final String name;
    private double balance;
    private double apr;
    private double dailyApr;
    private double minimumPayment;

    private int paymentsMade = 0;
    private double paymentsTotal = 0.0;
    private double interestCharged = 0.0;

    public CreditCard(String name, double balance, double aprPercent) {
        this.name = name;
        this.balance = balance;
        setApr(aprPercent);
        recalculateMinimumPayment();
    }

    public CreditCard(String name, double balance, double aprPercent, double minimumPayment) {
        this.name = name;
        this.balance = balance;
        setApr(aprPercent);
        this.minimumPayment = minimumPayment;
    }

    /**
     * Accepts the APR either as a percentage (18.99) or a fraction (0.1899).
     * The daily rate is the annual rate spread over the days in the current
     * year, so a leap year is handled without a special case.
     */
    private void setApr(double aprPercent) {
        this.apr = aprPercent >= 1 ? aprPercent / 100.0 : aprPercent;
        this.dailyApr = this.apr / Year.now().length();
    }

    /**
     * Interest for one billing month, compounded daily.
     *
     * The original version looped over the days adding dailyApr * balance while
     * never updating the balance, which made it simple interest with extra
     * steps. Each day's interest now joins the balance it is charged on, which
     * is the whole reason to iterate rather than multiply.
     *
     * This is a query: it reports the charge without applying it.
     */
    public double interestForMonth(int month, int year) {
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        double runningBalance = balance;
        double interest = 0.0;

        for (int day = 0; day < daysInMonth; day++) {
            double dailyInterest = runningBalance * dailyApr;
            interest += dailyInterest;
            runningBalance += dailyInterest;
        }
        return round(interest);
    }

    /** Apply one month's interest, then reset the minimum payment for the new balance. */
    public void bill(int month, int year) {
        double interest = interestForMonth(month, year);
        balance = round(balance + interest);
        interestCharged += interest;
        recalculateMinimumPayment();
    }

    /**
     * Pay an explicit amount. Never overpays: a payment larger than the balance
     * is trimmed to the balance, so the account cannot go negative.
     *
     * @return the amount actually applied
     */
    public double pay(double amount) {
        if (amount <= 0 || balance <= 0) {
            return 0.0;
        }
        double applied = Math.min(round(amount), balance);
        balance = round(balance - applied);
        paymentsMade++;
        paymentsTotal = round(paymentsTotal + applied);
        return applied;
    }

    /** Pay exactly the minimum due. */
    public double payMinimum() {
        return pay(minimumPayment);
    }

    private void recalculateMinimumPayment() {
        if (balance <= 0) {
            minimumPayment = 0.0;
            return;
        }
        double proportional = round(balance * MINIMUM_PAYMENT_RATE);
        minimumPayment = Math.min(Math.max(proportional, MINIMUM_PAYMENT_FLOOR), balance);
    }

    /** Round to cents. The original called Math.round and discarded the result. */
    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public boolean isPaidOff()        { return balance <= 0; }
    public String  getName()          { return name; }
    public double  getBalance()       { return balance; }
    public double  getApr()           { return apr; }
    public double  getDailyApr()      { return dailyApr; }
    public double  getMinimumPayment(){ return minimumPayment; }
    public int     getPaymentsMade()  { return paymentsMade; }
    public double  getPaymentsTotal() { return paymentsTotal; }
    public double  getInterestCharged() { return interestCharged; }
}
