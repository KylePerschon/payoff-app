# Payoff App

A debt snowball simulator. Give it a set of credit card balances, their APRs and
a fixed monthly budget, and it tells you how many months until every account is
clear and how much of what you paid was interest.

Built to answer a question no card statement will: if I can put a set amount
toward this every month, when does it actually end?

```
Starting balances (total $7320.00), budget $500.00/month

Account          Paid   Interest Payments
-----------------------------------------
Card A        5662.97     862.97       28
Card B         880.34      40.34        8
Card C         280.00       0.00        2
Card D        1400.00       0.00       11
-----------------------------------------
Total         8223.31     903.31

Debt free in 17 months (1 years, 5 months).
Interest was 11.0% of everything paid.
```

## How it models a month

Each cycle does four things in the order a real billing cycle does them:

1. **Interest accrues** on every account still carrying a balance
2. **Minimum payments** come out of the budget first — they aren't optional
3. **Everything left over** goes at the *smallest remaining balance*
4. The calendar advances

**Interest compounds daily.** The daily rate is the APR divided by the number of
days in the current year, so a leap year needs no special case, and each day's
interest joins the balance the next day is charged on. That's what makes the loop
over days worth writing instead of a single multiplication.

**Minimum payments follow the usual rule:** 2% of the balance, floored at $35,
capped at the balance itself so a nearly-clear account can't be asked for more than
it owes.

**The snowball overflow rolls onward.** When the extra payment would more than clear
the target account, only what's needed is applied and the remainder moves to the next
smallest balance in the same month. Without that, a month's surplus gets stranded on
an account that's already at zero.

## Snowball vs avalanche

This targets the smallest balance — the "snowball". Targeting the highest interest
rate instead — the "avalanche" — costs less in total interest, sometimes
meaningfully.

The snowball's argument is behavioural: an account closes sooner, and its minimum
payment joins the budget for the next target, so the thing visibly accelerates.

`findTarget()` is where that choice lives. Compare on `getApr()` instead of
`getBalance()` and you have the avalanche. Running both on the same inputs and
comparing total interest is the more interesting exercise, and is why the comparison
is isolated in one method.

## Running it

```bash
javac CreditCard.java PayoffSimulator.java
java PayoffSimulator
```

No dependencies, no build tool — plain Java, `java.time` only. Edit the card list
and `monthlyBudget` at the top of `PayoffSimulator.main`.

The figures in the file are examples. If the budget doesn't cover the combined
minimums, the simulator says so and stops rather than looping while the balances
grow.

## Tests

```bash
javac Sanity.java && java Sanity
```

Thirteen checks over the parts that are easy to get quietly wrong: APR accepted as
either a percentage or a fraction, the minimum payment floor and cap, interest
compounding above simple interest, February being longer in a leap year,
`interestForMonth` not mutating the balance it reports on, and payments neither
overpaying nor going negative.

## What changed from the first version

This began as a coursework exercise in 2021 and had a few real defects:

- **The month advanced twice per cycle** — once unconditionally and again inside an
  `else` — and the year never rolled over at all. Every month after the first used
  the wrong number of days, and the year stayed at its starting value indefinitely.
- **Interest didn't actually compound.** The loop added `dailyRate * balance` once
  per day without ever updating the balance, making it simple interest computed the
  long way round.
- `Math.round(...)` was called and its return value discarded, so the rounding it
  was there to do never happened.
- Balances were compared with `==` on boxed `Double` objects, which tests identity
  rather than value.
- The per-account logic was copy-pasted four times against named variables while a
  `CreditCard[]` array and a `lowestBalance()` helper sat unused beside it. Adding a
  fifth card meant another paste.

The accounts are now a real array the loops iterate, and each card tracks its own
payment count, total paid and interest charged.

## A note

This is a modelling exercise, not financial advice. It assumes a fixed budget, no
new spending on any account, and a constant APR — a promotional 0% that expires, a
rate change, or one unplanned purchase will move the answer. Use it to see the shape
of the problem, not to plan on the cent.
