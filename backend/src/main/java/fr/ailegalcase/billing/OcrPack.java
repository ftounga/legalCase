package fr.ailegalcase.billing;

/**
 * Packs de pages OCR achetables en one-shot (SF-122-04).
 * Les price IDs Stripe sont configurés dans application.yml.
 *
 * Marges : OCR_500 64 %, OCR_2000 53 %, OCR_8000 45 %.
 */
public enum OcrPack {
    OCR_500(500, 1900),
    OCR_2000(2_000, 5900),
    OCR_8000(8_000, 19900);

    private final int pages;
    private final int amountCents;

    OcrPack(int pages, int amountCents) {
        this.pages = pages;
        this.amountCents = amountCents;
    }

    public int getPages() { return pages; }
    public int getAmountCents() { return amountCents; }

    /** Détecte si un packCode correspond à un pack OCR. */
    public static boolean isOcrPack(String packCode) {
        return packCode != null && packCode.startsWith("OCR_");
    }
}
