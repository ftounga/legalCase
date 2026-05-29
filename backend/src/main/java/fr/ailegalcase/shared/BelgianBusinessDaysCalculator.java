package fr.ailegalcase.shared;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * SF-DT-27-01 : utilitaire de calcul des jours ouvrables belges.
 *
 * Un jour ouvrable en Belgique au sens de la Loi du 03/07/1978 sur les contrats de travail
 * est un jour de la semaine (lundi à samedi) qui n'est pas un jour férié légal.
 *
 * <p>Pour la SF-DT-27-01 (vérification des délais de 3 jours ouvrables art. 35),
 * la convention retenue est : samedi + dimanche + 10 jours fériés belges exclus,
 * conformément à la pratique jurisprudentielle courante du tribunal du travail.
 *
 * <p>Jours fériés belges (10 légaux) :
 * <ul>
 *   <li>Jour de l'An — 1/1</li>
 *   <li>Lundi de Pâques — algorithme Meeus/Jones/Butcher</li>
 *   <li>Fête du Travail — 1/5</li>
 *   <li>Ascension — Pâques + 39 jours</li>
 *   <li>Lundi de Pentecôte — Pâques + 50 jours</li>
 *   <li>Fête nationale — 21/7</li>
 *   <li>Assomption — 15/8</li>
 *   <li>Toussaint — 1/11</li>
 *   <li>Armistice — 11/11</li>
 *   <li>Noël — 25/12</li>
 * </ul>
 *
 * <p>Le jour {@code from} est <b>exclu</b> (jour de départ du délai — art. 35 :
 * on compte à partir du lendemain de la connaissance du fait). Le jour {@code to}
 * est <b>inclus</b>.
 */
public final class BelgianBusinessDaysCalculator {

    private BelgianBusinessDaysCalculator() {
    }

    /**
     * Nombre de jours ouvrables entre deux dates, {@code from} exclu, {@code to} inclus.
     *
     * @throws IllegalArgumentException si l'une des dates est nulle ou si {@code to} est avant {@code from}
     */
    public static int countBusinessDays(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Les dates 'from' et 'to' sont requises");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La date 'to' ne peut être antérieure à 'from'");
        }
        int count = 0;
        LocalDate cursor = from.plusDays(1);
        while (!cursor.isAfter(to)) {
            if (isBusinessDay(cursor)) {
                count++;
            }
            cursor = cursor.plusDays(1);
        }
        return count;
    }

    /**
     * Ajoute {@code n} jours ouvrables belges à une date. Le jour {@code start}
     * est <b>exclu</b> : le résultat est le n-ième jour ouvrable strict après
     * {@code start} (au sens de {@link #isBusinessDay(LocalDate)}).
     *
     * <p>Utilisé par les délais procéduraux comptés en jours ouvrables — recours
     * en extrême urgence CCE (5 jours ouvrables, F-IM-32 / SF-215-15) et recours
     * Annexe 13 (F-IM-08).
     *
     * @throws IllegalArgumentException si {@code start} est nul.
     */
    public static LocalDate addBusinessDays(LocalDate start, int n) {
        if (start == null) {
            throw new IllegalArgumentException("La date 'start' est requise");
        }
        if (n <= 0) {
            return start;
        }
        LocalDate cursor = start;
        int remaining = n;
        while (remaining > 0) {
            cursor = cursor.plusDays(1);
            if (isBusinessDay(cursor)) {
                remaining--;
            }
        }
        return cursor;
    }

    /**
     * Indique si une date donnée est un jour ouvrable belge.
     * Exclut samedi, dimanche et jours fériés.
     */
    public static boolean isBusinessDay(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !isPublicHoliday(date);
    }

    /**
     * Indique si une date est un jour férié légal belge.
     */
    public static boolean isPublicHoliday(LocalDate date) {
        if (date == null) {
            return false;
        }
        return holidaysForYear(date.getYear()).contains(date);
    }

    /**
     * Liste des 10 jours fériés légaux belges pour une année donnée.
     */
    public static Set<LocalDate> holidaysForYear(int year) {
        Set<LocalDate> holidays = new HashSet<>();
        holidays.add(LocalDate.of(year, 1, 1));     // Jour de l'An
        holidays.add(LocalDate.of(year, 5, 1));     // Fête du Travail
        holidays.add(LocalDate.of(year, 7, 21));    // Fête nationale
        holidays.add(LocalDate.of(year, 8, 15));    // Assomption
        holidays.add(LocalDate.of(year, 11, 1));    // Toussaint
        holidays.add(LocalDate.of(year, 11, 11));   // Armistice
        holidays.add(LocalDate.of(year, 12, 25));   // Noël

        LocalDate easter = computeEasterDate(year);
        holidays.add(easter.plusDays(1));   // Lundi de Pâques
        holidays.add(easter.plusDays(39));  // Ascension
        holidays.add(easter.plusDays(50));  // Lundi de Pentecôte
        return holidays;
    }

    /**
     * Algorithme de Meeus/Jones/Butcher pour le calcul de la date de Pâques (grégorien).
     */
    public static LocalDate computeEasterDate(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
