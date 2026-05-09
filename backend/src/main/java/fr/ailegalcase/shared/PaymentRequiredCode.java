package fr.ailegalcase.shared;

/**
 * Codes machine-readable des réponses HTTP 402 PAYMENT_REQUIRED.
 * Utilisé par le frontend pour router le rendu (bandeau, message, CTA upgrade)
 * sans parser le message libre.
 */
public enum PaymentRequiredCode {
    TOKEN_BUDGET_EXCEEDED,
    CASE_ANALYSIS_LIMIT_EXCEEDED,
    CHAT_MESSAGE_LIMIT_EXCEEDED,
    DOCUMENT_LIMIT_EXCEEDED,
    CASE_FILE_OPEN_LIMIT_EXCEEDED,
    OCR_QUOTA_EXCEEDED,
    SEAT_LIMIT_EXCEEDED,
    /** SF-231-03 : quota mensuel minutes vidéo dépassé (SOLO 5, TEAM 30, PRO 120). */
    VIDEO_QUOTA_EXCEEDED
}
