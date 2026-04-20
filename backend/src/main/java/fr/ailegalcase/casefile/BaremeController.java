package fr.ailegalcase.casefile;

import fr.ailegalcase.referential.LegalReferentialService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/anciennete/baremes")
public class BaremeController {

    private final LegalReferentialService legalReferentialService;

    public BaremeController(LegalReferentialService legalReferentialService) {
        this.legalReferentialService = legalReferentialService;
    }

    /**
     * SF-129-01 : lit via LegalReferentialService (DB-first, fallback static).
     * Supporte les nouveaux codes IDCC_XXXX seedés en DB ET les codes legacy
     * (METALLURGIE, COMMERCE, BTP, HCR, SYNTEC) du static file.
     */
    @GetMapping("/{conventionCode}")
    public BaremeResponse get(@PathVariable String conventionCode) {
        String normalized = ConventionCodeNormalizer.normalize(conventionCode);
        ConventionBareme bareme = legalReferentialService.getConventionBareme(
                normalized != null ? normalized : conventionCode);
        if (bareme == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Convention inconnue : " + conventionCode);
        }
        return new BaremeResponse(
                bareme.code(),
                bareme.label(),
                bareme.country(),
                bareme.congesLegauxJours(),
                bareme.congesSupplementaires().stream()
                        .map(c -> new BaremeResponse.CongesSupplementaireData(
                                c.ancienneteMinAnnees(), c.joursSupplementaires())).toList(),
                bareme.primesAnciennete().stream()
                        .map(p -> new BaremeResponse.PrimeAncienneteData(
                                p.ancienneteMinAnnees(), p.pourcentage())).toList()
        );
    }
}
