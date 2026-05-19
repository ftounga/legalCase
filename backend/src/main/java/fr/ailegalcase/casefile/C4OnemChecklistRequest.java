package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-207-02 : payload de création/upsert d'une analyse de conformité C4 ONEM.
 *
 * <p>Les champs marqués obligatoires côté contrat ({@code nomSalarie},
 * {@code dateEntreeService}, {@code dateSortieService},
 * {@code fauteGraveMentionnee}) sont validés par Bean Validation (400 si
 * absents).</p>
 *
 * <p>Les autres champs peuvent être absents du document C4 — leur absence
 * est interprétée comme une <i>mention manquante</i> par le calculateur
 * (verdict {@code NON_CONFORME}). Le calculateur fait également office de
 * second filtre pour les formats — un {@code numeroBce} fourni mais
 * malformé (≠ 10 chiffres) déclenche un 400 côté service.</p>
 *
 * @see C4OnemChecklistCalculator
 */
public record C4OnemChecklistRequest(
        String raisonSocialeEmployeur,
        String numeroBce,
        @NotNull(message = "nomSalarie est requis") String nomSalarie,
        String numeroNationalRegistre,
        @NotNull(message = "dateEntreeService est requise") LocalDate dateEntreeService,
        @NotNull(message = "dateSortieService est requise") LocalDate dateSortieService,
        String categorieOnem,
        String motifExplicite,
        @NotNull(message = "fauteGraveMentionnee est requis (boolean)") Boolean fauteGraveMentionnee,
        Integer preavisPresteJours,
        BigDecimal dernierSalaireMensuelBrut
) {}
