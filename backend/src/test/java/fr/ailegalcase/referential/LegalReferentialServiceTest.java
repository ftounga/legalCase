package fr.ailegalcase.referential;

import fr.ailegalcase.casefile.ImmigrationProcedureReferentiel.ProcedureJalon;
import fr.ailegalcase.casefile.LitigationTypeMapper.LitigationPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

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
}
