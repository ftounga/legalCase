package fr.ailegalcase.billing;

/**
 * Packs de tokens achetables en one-shot.
 * Les price IDs Stripe sont configurés dans application.yml.
 */
public enum TokenPack {
    TOKENS_1M(1_000_000L, 990),
    TOKENS_5M(5_000_000L, 3990),
    TOKENS_20M(20_000_000L, 12990);

    private final long tokens;
    private final int amountCents;

    TokenPack(long tokens, int amountCents) {
        this.tokens = tokens;
        this.amountCents = amountCents;
    }

    public long getTokens() { return tokens; }
    public int getAmountCents() { return amountCents; }
}
