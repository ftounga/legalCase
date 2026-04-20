package fr.ailegalcase.billing;

/**
 * SF-123-03 : résumé seats pour la page billing + header membres.
 * Prix exprimés en centimes TTC pour éviter les arrondis côté client.
 */
public record SeatsSummaryResponse(
        String planCode,
        int seatCount,
        int includedSeats,
        int maxSeats,
        int extraSeatPriceCents,
        int baseMonthlyCostCents,
        int totalMonthlyCostCents
) {}
