package fr.ailegalcase.referential;

import fr.ailegalcase.casefile.ConventionBareme;
import fr.ailegalcase.casefile.ExpectedPiece;
import fr.ailegalcase.casefile.ImmigrationProcedureReferentiel.ProcedureJalon;
import fr.ailegalcase.casefile.LitigationTypeMapper.LitigationPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class LegalReferentialServiceTest {

    private LegalReferentialRepository repository;
    private LegalReferentialService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LegalReferentialRepository.class);
        service = new LegalReferentialService(repository);
    }

    // --- getLitigationPeriod ---

    @Test
    void getLitigationPeriod_retourne_donnees_DB_si_presentes() {
        LegalReferential entry = new LegalReferential();
        entry.setValueJson("{\"years\":1,\"article\":\"Art. L1471-1\"}");
        entry.setLabel("Licenciement sans cause réelle et sérieuse");
        when(repository.findSystemEntry("DROIT_DU_TRAVAIL", "LITIGATION_TYPE", "LICENCIEMENT_SANS_CAUSE_REELLE"))
                .thenReturn(List.of(entry));

        Optional<LitigationPeriod> result = service.getLitigationPeriod("LICENCIEMENT_SANS_CAUSE_REELLE");

        assertThat(result).isPresent();
        assertThat(result.get().years()).isEqualTo(1);
        assertThat(result.get().article()).isEqualTo("Art. L1471-1");
        assertThat(result.get().label()).isEqualTo("Licenciement sans cause réelle et sérieuse");
    }

    @Test
    void getLitigationPeriod_fallback_Java_si_DB_vide() {
        when(repository.findSystemEntry(any(), any(), any())).thenReturn(List.of());

        Optional<LitigationPeriod> result = service.getLitigationPeriod("DISCRIMINATION");

        assertThat(result).isPresent();
        assertThat(result.get().years()).isEqualTo(5);
    }

    @Test
    void getLitigationPeriod_retourne_empty_si_type_null() {
        Optional<LitigationPeriod> result = service.getLitigationPeriod(null);
        assertThat(result).isEmpty();
    }

    // --- getImmigrationJalons ---

    @Test
    void getImmigrationJalons_retourne_donnees_DB_si_presentes() {
        LegalReferential entry = new LegalReferential();
        entry.setValueJson("[{\"label\":\"Instruction préfecture\",\"offsetDays\":180},{\"label\":\"Silence vaut rejet\",\"offsetDays\":120}]");
        when(repository.findSystemEntry("DROIT_IMMIGRATION", "IMMIGRATION_JALONS", "REGULARISATION_EXCEPTIONNELLE"))
                .thenReturn(List.of(entry));

        List<ProcedureJalon> jalons = service.getImmigrationJalons("REGULARISATION_EXCEPTIONNELLE");

        assertThat(jalons).hasSize(2);
        assertThat(jalons.get(0).label()).isEqualTo("Instruction préfecture");
        assertThat(jalons.get(0).offsetDays()).isEqualTo(180);
        assertThat(jalons.get(1).offsetDays()).isEqualTo(120);
    }

    @Test
    void getImmigrationJalons_fallback_Java_si_DB_vide() {
        when(repository.findSystemEntry(any(), any(), any())).thenReturn(List.of());

        List<ProcedureJalon> jalons = service.getImmigrationJalons("RENOUVELLEMENT_TITRE_SEJOUR");

        assertThat(jalons).isNotEmpty();
        assertThat(jalons.get(0).offsetDays()).isEqualTo(120);
    }

    @Test
    void getImmigrationJalons_retourne_liste_vide_si_type_null() {
        List<ProcedureJalon> jalons = service.getImmigrationJalons(null);
        assertThat(jalons).isEmpty();
    }

    // --- getConventionBareme (SF-129-03 : DB only + normalizer) ---

    private LegalReferential metallurgieDbEntry() {
        LegalReferential e = new LegalReferential();
        e.setEntryKey("IDCC_3248");
        e.setLabel("Métallurgie (IDCC 3248)");
        e.setCountry("FRANCE");
        e.setSourceRef("Convention collective nationale de la métallurgie (IDCC 3248)");
        e.setValueJson("""
                {
                  "congesLegauxJours": 25,
                  "congesSupp": [
                    {"min": 5, "jours": 1},
                    {"min": 10, "jours": 2}
                  ],
                  "primes": [
                    {"min": 3, "pct": 3},
                    {"min": 9, "pct": 9}
                  ]
                }
                """);
        return e;
    }

    @Test
    void getConventionBareme_idccCode_returnsDbBareme() {
        when(repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_BAREMES", "IDCC_3248"))
                .thenReturn(List.of(metallurgieDbEntry()));

        ConventionBareme bareme = service.getConventionBareme("IDCC_3248");

        assertThat(bareme).isNotNull();
        assertThat(bareme.code()).isEqualTo("IDCC_3248");
        assertThat(bareme.country()).isEqualTo("FRANCE");
        assertThat(bareme.congesLegauxJours()).isEqualTo(25);
        assertThat(bareme.congesSupplementaires()).hasSize(2);
        assertThat(bareme.primesAnciennete()).hasSize(2);
    }

    @Test
    void getConventionBareme_legacyCode_normalizesAndReturnsDbBareme() {
        // Le normalizer doit transformer METALLURGIE en IDCC_3248 avant le lookup DB
        when(repository.findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_BAREMES", "IDCC_3248"))
                .thenReturn(List.of(metallurgieDbEntry()));

        ConventionBareme bareme = service.getConventionBareme("METALLURGIE");

        assertThat(bareme).isNotNull();
        assertThat(bareme.code()).isEqualTo("IDCC_3248");
        verify(repository).findSystemEntry("DROIT_DU_TRAVAIL", "CONVENTION_BAREMES", "IDCC_3248");
    }

    @Test
    void getConventionBareme_unknownCode_returnsNull() {
        // SF-129-03 : plus de fallback statique — code inconnu → null
        when(repository.findSystemEntry(any(), any(), any())).thenReturn(List.of());

        ConventionBareme bareme = service.getConventionBareme("INCONNU_XYZ");

        assertThat(bareme).isNull();
    }

    @Test
    void getConventionBareme_nullInput_returnsNull() {
        ConventionBareme bareme = service.getConventionBareme(null);

        assertThat(bareme).isNull();
        verify(repository, never()).findSystemEntry(any(), any(), any());
    }

    @Test
    void getConventionBareme_blankInput_returnsNull() {
        ConventionBareme bareme = service.getConventionBareme("   ");

        assertThat(bareme).isNull();
        verify(repository, never()).findSystemEntry(any(), any(), any());
    }

    // --- getReferentials ---

    @Test
    void getReferentials_groupe_entrees_par_type() {
        LegalReferential e1 = new LegalReferential();
        e1.setReferentialType("LITIGATION_TYPE");
        e1.setEntryKey("DISCRIMINATION");
        e1.setLabel("Discrimination");
        e1.setValueJson("{\"years\":5,\"article\":\"Art. L1132-1\"}");
        e1.setSystem(true);

        LegalReferential e2 = new LegalReferential();
        e2.setReferentialType("BAREME_MACRON");
        e2.setEntryKey("LICENCIEMENT");
        e2.setLabel("Licenciement");
        e2.setValueJson("{\"supported\":true}");
        e2.setSystem(true);

        when(repository.findActiveByDomain(eq("DROIT_DU_TRAVAIL"), any(UUID.class)))
                .thenReturn(List.of(e1, e2));

        ReferentialResponse response = service.getReferentials("DROIT_DU_TRAVAIL", UUID.randomUUID());

        assertThat(response.domain()).isEqualTo("DROIT_DU_TRAVAIL");
        assertThat(response.sections()).containsKey("LITIGATION_TYPE");
        assertThat(response.sections()).containsKey("BAREME_MACRON");
        assertThat(response.sections().get("LITIGATION_TYPE")).hasSize(1);
        assertThat(response.sections().get("LITIGATION_TYPE").get(0).key()).isEqualTo("DISCRIMINATION");
    }

    @Test
    void getReferentials_retourne_sections_vides_si_aucune_entree() {
        when(repository.findActiveByDomain(any(), any())).thenReturn(List.of());

        ReferentialResponse response = service.getReferentials("DROIT_FAMILLE", UUID.randomUUID());

        assertThat(response.domain()).isEqualTo("DROIT_FAMILLE");
        assertThat(response.sections()).isEmpty();
    }

    // --- SF-137-01 : filtrage pays workspace ---

    private LegalReferential sysEntry(String type, String key, String country, String value) {
        LegalReferential e = new LegalReferential();
        e.setReferentialType(type);
        e.setEntryKey(key);
        e.setLabel(key + " label");
        e.setCountry(country);
        e.setValueJson(value);
        e.setSystem(true);
        e.setActive(true);
        return e;
    }

    @Test
    void getReferentials_workspaceFR_masque_entries_BE_garde_globales_et_FR() {
        var conventionFR = sysEntry("CONVENTION_BAREMES", "IDCC_3248", "FRANCE", "{}");
        var conventionBE = sysEntry("CONVENTION_BAREMES", "CP200", "BELGIQUE", "{}");
        var litigeGlobal = sysEntry("LITIGATION_TYPE", "DISCRIMINATION", null, "{}");
        when(repository.findActiveByDomain(any(), any())).thenReturn(List.of(conventionFR, conventionBE, litigeGlobal));

        ReferentialResponse response = service.getReferentials("DROIT_DU_TRAVAIL", UUID.randomUUID(), "FRANCE");

        assertThat(response.sections().get("CONVENTION_BAREMES"))
                .extracting(ReferentialResponse.Entry::key)
                .containsExactly("IDCC_3248");
        assertThat(response.sections().get("LITIGATION_TYPE"))
                .extracting(ReferentialResponse.Entry::key)
                .containsExactly("DISCRIMINATION");
    }

    @Test
    void getReferentials_workspaceBE_masque_entries_FR_garde_globales_et_BE() {
        var conventionFR = sysEntry("CONVENTION_BAREMES", "IDCC_3248", "FRANCE", "{}");
        var conventionBE = sysEntry("CONVENTION_BAREMES", "CP200", "BELGIQUE", "{}");
        var litigeGlobal = sysEntry("LITIGATION_TYPE", "DISCRIMINATION", null, "{}");
        when(repository.findActiveByDomain(any(), any())).thenReturn(List.of(conventionFR, conventionBE, litigeGlobal));

        ReferentialResponse response = service.getReferentials("DROIT_DU_TRAVAIL", UUID.randomUUID(), "BELGIQUE");

        assertThat(response.sections().get("CONVENTION_BAREMES"))
                .extracting(ReferentialResponse.Entry::key)
                .containsExactly("CP200");
        assertThat(response.sections().get("LITIGATION_TYPE"))
                .extracting(ReferentialResponse.Entry::key)
                .containsExactly("DISCRIMINATION");
    }

    @Test
    void getReferentials_workspaceCountryNull_noFiltering() {
        var conventionFR = sysEntry("CONVENTION_BAREMES", "IDCC_3248", "FRANCE", "{}");
        var conventionBE = sysEntry("CONVENTION_BAREMES", "CP200", "BELGIQUE", "{}");
        when(repository.findActiveByDomain(any(), any())).thenReturn(List.of(conventionFR, conventionBE));

        // Signature 2-arg préserve rétrocompat (ne filtre pas)
        ReferentialResponse response = service.getReferentials("DROIT_DU_TRAVAIL", UUID.randomUUID());

        assertThat(response.sections().get("CONVENTION_BAREMES"))
                .extracting(ReferentialResponse.Entry::key)
                .containsExactlyInAnyOrder("IDCC_3248", "CP200");
    }

    // --- updateReferential ---

    private ReferentialValidationService mockValidationOk() {
        ReferentialValidationService vs = Mockito.mock(ReferentialValidationService.class);
        when(vs.validate(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ReferentialValidationService.ValidationResult(true, null));
        return vs;
    }

    private LegalReferential systemEntry(UUID id) {
        LegalReferential e = new LegalReferential();
        e.setId(id);
        e.setWorkspaceId(null);
        e.setLegalDomain("DROIT_DU_TRAVAIL");
        e.setReferentialType("LITIGATION_TYPE");
        e.setEntryKey("DISCRIMINATION");
        e.setLabel("Discrimination");
        e.setValueJson("{\"years\":5}");
        e.setSystem(true);
        e.setActive(true);
        return e;
    }

    // UPD-01 : entrée système → crée workspace override
    @Test
    void updateReferential_systemEntry_createsWorkspaceOverride() {
        UUID entryId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LegalReferential sys = systemEntry(entryId);

        when(repository.findById(entryId)).thenReturn(Optional.of(sys));
        when(repository.findWorkspaceEntry(eq(workspaceId), any(), any(), any(), any()))
                .thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateReferential(entryId, workspaceId, userId,
                "Discrimination (modifié)", "{\"years\":4}", false, mockValidationOk());

        assertThat(response.saved()).isTrue();
        ArgumentCaptor<LegalReferential> captor = ArgumentCaptor.forClass(LegalReferential.class);
        verify(repository).save(captor.capture());
        LegalReferential saved = captor.getValue();
        assertThat(saved.getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(saved.isSystem()).isFalse();
        assertThat(saved.getValueJson()).isEqualTo("{\"years\":4}");
        // Label système verrouillé : newLabel ignoré, label source conservé
        assertThat(saved.getLabel()).isEqualTo("Discrimination");
    }

    // UPD-02 : entrée workspace → update in-place
    @Test
    void updateReferential_workspaceEntry_updatesInPlace() {
        UUID entryId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        LegalReferential wsEntry = systemEntry(entryId);
        wsEntry.setWorkspaceId(workspaceId);
        wsEntry.setSystem(false);

        when(repository.findById(entryId)).thenReturn(Optional.of(wsEntry));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.updateReferential(entryId, workspaceId, userId,
                "Discrimination (modifié)", "{\"years\":4}", false, mockValidationOk());

        assertThat(response.saved()).isTrue();
        verify(repository, never()).findWorkspaceEntry(any(), any(), any(), any(), any());
    }

    // UPD-03 : getReferentials déduplique — workspace override masque système
    @Test
    void getReferentials_deduplicates_workspaceOverrideWinsOverSystem() {
        LegalReferential sys = new LegalReferential();
        sys.setReferentialType("LITIGATION_TYPE");
        sys.setEntryKey("DISCRIMINATION");
        sys.setLabel("Discrimination (système)");
        sys.setValueJson("{\"years\":5}");
        sys.setSystem(true);

        LegalReferential ws = new LegalReferential();
        ws.setReferentialType("LITIGATION_TYPE");
        ws.setEntryKey("DISCRIMINATION");
        ws.setLabel("Discrimination (custom)");
        ws.setValueJson("{\"years\":4}");
        ws.setSystem(false);

        when(repository.findActiveByDomain(any(), any())).thenReturn(List.of(sys, ws));

        ReferentialResponse response = service.getReferentials("DROIT_DU_TRAVAIL", UUID.randomUUID());

        List<ReferentialResponse.Entry> entries = response.sections().get("LITIGATION_TYPE");
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).label()).isEqualTo("Discrimination (custom)");
        assertThat(entries.get(0).valueJson()).isEqualTo("{\"years\":4}");
    }

    // ==================================================================
    //  F-294 SF-294-01 — getExpectedPieces
    // ==================================================================

    private LegalReferential expectedPieceEntry(String key, String label, String country,
                                                String valueJson, boolean system) {
        LegalReferential e = new LegalReferential();
        e.setReferentialType("EXPECTED_PIECES");
        e.setEntryKey(key);
        e.setLabel(label);
        e.setCountry(country);
        e.setValueJson(valueJson);
        e.setSystem(system);
        return e;
    }

    @Test
    void getExpectedPieces_dbPresent_filteredByStage() {
        LegalReferential lettre = expectedPieceEntry("LETTRE_LICENCIEMENT", "Lettre de licenciement",
                "FRANCE", "{\"stages\":[\"FOND\"],\"obligatoire\":true,\"ordre\":3}", true);
        LegalReferential appelOnly = expectedPieceEntry("CONCLUSIONS_APPEL", "Conclusions d'appel",
                "FRANCE", "{\"stages\":[\"APPEL\"],\"obligatoire\":true,\"ordre\":9}", true);
        when(repository.findActiveByDomainAndType("DROIT_DU_TRAVAIL", "EXPECTED_PIECES", null))
                .thenReturn(List.of(lettre, appelOnly));

        var result = service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", "FOND");

        assertThat(result).extracting("label").containsExactly("Lettre de licenciement");
    }

    @Test
    void getExpectedPieces_genericPiece_includedWhateverStage_andWhenStageNull() {
        LegalReferential convCol = expectedPieceEntry("CONVENTION_COLLECTIVE", "Convention collective applicable",
                "FRANCE", "{\"obligatoire\":true,\"ordre\":8}", true);
        when(repository.findActiveByDomainAndType("DROIT_DU_TRAVAIL", "EXPECTED_PIECES", null))
                .thenReturn(List.of(convCol));

        assertThat(service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", "FOND"))
                .extracting("label").containsExactly("Convention collective applicable");
        // stade null → seules les génériques (CA5)
        assertThat(service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", null))
                .extracting("label").containsExactly("Convention collective applicable");
    }

    @Test
    void getExpectedPieces_stagedPiece_excludedWhenStageNull() {
        LegalReferential lettre = expectedPieceEntry("LETTRE_LICENCIEMENT", "Lettre de licenciement",
                "FRANCE", "{\"stages\":[\"FOND\"],\"obligatoire\":true,\"ordre\":3}", true);
        when(repository.findActiveByDomainAndType("DROIT_DU_TRAVAIL", "EXPECTED_PIECES", null))
                .thenReturn(List.of(lettre));

        assertThat(service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", null)).isEmpty();
    }

    @Test
    void getExpectedPieces_dbEmpty_fallbackJava() {
        when(repository.findActiveByDomainAndType("DROIT_DU_TRAVAIL", "EXPECTED_PIECES", null))
                .thenReturn(List.of());

        var result = service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", "FOND");

        // fallback TravailPieceReferentiel : 7 pièces 1re instance + 1 générique
        assertThat(result).extracting("label")
                .contains("Lettre de licenciement", "Contrat de travail",
                        "Convention collective applicable");
    }

    @Test
    void getExpectedPieces_workspaceOverrideWinsOverSystem() {
        LegalReferential sys = expectedPieceEntry("LETTRE_LICENCIEMENT", "Lettre (système)",
                "FRANCE", "{\"stages\":[\"FOND\"],\"obligatoire\":true,\"ordre\":3}", true);
        LegalReferential ws = expectedPieceEntry("LETTRE_LICENCIEMENT", "Lettre (custom workspace)",
                "FRANCE", "{\"stages\":[\"FOND\"],\"obligatoire\":true,\"ordre\":3}", false);
        when(repository.findActiveByDomainAndType("DROIT_DU_TRAVAIL", "EXPECTED_PIECES", null))
                .thenReturn(List.of(sys, ws));

        var result = service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", "FOND");

        assertThat(result).extracting("label").containsExactly("Lettre (custom workspace)");
    }

    @Test
    void getExpectedPieces_domainNotCovered_returnsEmpty() {
        // DROIT_IMMIGRATION : ni DB EXPECTED_PIECES ni fallback Java (hors scope F-294)
        when(repository.findActiveByDomainAndType("DROIT_IMMIGRATION", "EXPECTED_PIECES", null))
                .thenReturn(List.of());

        assertThat(service.getExpectedPieces("DROIT_IMMIGRATION", "FRANCE", "FOND")).isEmpty();
    }

    @Test
    void getExpectedPieces_malformedJson_failsOpen() {
        LegalReferential bad = expectedPieceEntry("LETTRE_LICENCIEMENT", "Lettre de licenciement",
                "FRANCE", "{not-json", true);
        when(repository.findActiveByDomainAndType("DROIT_DU_TRAVAIL", "EXPECTED_PIECES", null))
                .thenReturn(List.of(bad));

        // L'entrée mal formée est ignorée → DB "vide" de pièces valides → fallback Java.
        var result = service.getExpectedPieces("DROIT_DU_TRAVAIL", "FRANCE", "FOND");
        assertThat(result).isNotNull();
        assertThat(result).extracting("label").contains("Lettre de licenciement");
    }

    @Test
    void getExpectedPieces_nullArgs_returnsEmpty() {
        assertThat(service.getExpectedPieces(null, "FRANCE", "FOND")).isEmpty();
        assertThat(service.getExpectedPieces("DROIT_DU_TRAVAIL", null, "FOND")).isEmpty();
    }

    // ==================================================================
    //  F-294 SF-294-03 — Famille : réutilisation de DIVORCE_PIECES
    // ==================================================================

    private LegalReferential divorcePieceEntry(String key, String label, String description,
                                               boolean obligatoire) {
        LegalReferential e = new LegalReferential();
        e.setReferentialType("DIVORCE_PIECES");
        e.setEntryKey(key);
        e.setLabel(label);
        e.setValueJson("{\"description\":\"" + description + "\",\"obligatoire\":" + obligatoire + "}");
        e.setSystem(true);
        return e;
    }

    // CA1 : Famille FR → pièces divorce FR mappées en ExpectedPiece (fallback Java).
    @Test
    void getExpectedPieces_famille_FR_mapsDivorcePieces() {
        when(repository.findSystemEntriesByTypeAndCountry("DROIT_FAMILLE", "DIVORCE_PIECES", "FRANCE"))
                .thenReturn(List.of());

        var result = service.getExpectedPieces("DROIT_FAMILLE", "FRANCE", null);

        assertThat(result).isNotEmpty();
        assertThat(result).extracting("label")
                .contains("Copie intégrale de l'acte de mariage",
                        "Actes de naissance des époux",
                        "Pièces d'identité des deux époux");
        assertThat(result).extracting("country").containsOnly("FRANCE");
        // pièces génériques (stages vides) → incluses quel que soit le stade
        assertThat(result).allMatch(ExpectedPiece::isGenerique);
        // ordre = index commençant à 1, croissant
        assertThat(result.get(0).ordre()).isEqualTo(1);
        assertThat(result.get(1).ordre()).isEqualTo(2);
    }

    // CA2 : Famille BE → pièces divorce BE.
    @Test
    void getExpectedPieces_famille_BE_mapsDivorcePieces() {
        when(repository.findSystemEntriesByTypeAndCountry("DROIT_FAMILLE", "DIVORCE_PIECES", "BELGIQUE"))
                .thenReturn(List.of());

        var result = service.getExpectedPieces("DROIT_FAMILLE", "BELGIQUE", "FOND");

        assertThat(result).isNotEmpty();
        assertThat(result).extracting("label")
                .contains("Copie de l'acte de mariage",
                        "Certificat de composition de ménage");
        assertThat(result).extracting("country").containsOnly("BELGIQUE");
    }

    // CA1 : un procedureStage arbitraire renvoie les MÊMES pièces (génériques).
    @Test
    void getExpectedPieces_famille_stageIgnored_sameGenericPieces() {
        when(repository.findSystemEntriesByTypeAndCountry("DROIT_FAMILLE", "DIVORCE_PIECES", "FRANCE"))
                .thenReturn(List.of());

        var withStage = service.getExpectedPieces("DROIT_FAMILLE", "FRANCE", "MESURES_PROVISOIRES");
        var withoutStage = service.getExpectedPieces("DROIT_FAMILLE", "FRANCE", null);
        var otherStage = service.getExpectedPieces("DROIT_FAMILLE", "FRANCE", "FOND");

        assertThat(withStage).extracting("label")
                .isEqualTo(withoutStage.stream().map(ExpectedPiece::label).toList());
        assertThat(otherStage).extracting("label")
                .isEqualTo(withoutStage.stream().map(ExpectedPiece::label).toList());
    }

    // CA3 : DB DIVORCE_PIECES prime sur le fallback Java (via getDivorcePieces).
    @Test
    void getExpectedPieces_famille_dbDivorcePiecesWinOverFallback() {
        LegalReferential dbPiece = divorcePieceEntry("FR_ACTE_MARIAGE",
                "Acte de mariage (version DB)", "Moins de 6 mois", true);
        when(repository.findSystemEntriesByTypeAndCountry("DROIT_FAMILLE", "DIVORCE_PIECES", "FRANCE"))
                .thenReturn(List.of(dbPiece));

        var result = service.getExpectedPieces("DROIT_FAMILLE", "FRANCE", null);

        // une seule entrée DB → prime sur les 9 pièces du fallback Java
        assertThat(result).hasSize(1);
        assertThat(result.get(0).label()).isEqualTo("Acte de mariage (version DB)");
        assertThat(result.get(0).code()).isEqualTo("FR_ACTE_MARIAGE");
        assertThat(result.get(0).country()).isEqualTo("FRANCE");
        assertThat(result.get(0).isGenerique()).isTrue();
    }

    // CA7 : pays non couvert → liste vide, pas d'exception.
    @Test
    void getExpectedPieces_famille_countryNotCovered_returnsEmpty() {
        when(repository.findSystemEntriesByTypeAndCountry("DROIT_FAMILLE", "DIVORCE_PIECES", "ALLEMAGNE"))
                .thenReturn(List.of());

        assertThat(service.getExpectedPieces("DROIT_FAMILLE", "ALLEMAGNE", "FOND")).isEmpty();
    }

    // CA7 : exception dans la résolution → fail-open (liste vide), run abouti.
    @Test
    void getExpectedPieces_famille_repositoryThrows_failsOpen() {
        // getDivorcePieces avale l'exception et retombe sur le fallback Java ;
        // pour exercer le fail-open propre à la branche Famille, on couvre le pays
        // non géré par le fallback Java après exception DB.
        when(repository.findSystemEntriesByTypeAndCountry("DROIT_FAMILLE", "DIVORCE_PIECES", "ALLEMAGNE"))
                .thenThrow(new RuntimeException("DB down"));

        var result = service.getExpectedPieces("DROIT_FAMILLE", "ALLEMAGNE", "FOND");
        assertThat(result).isNotNull().isEmpty();
    }
}
