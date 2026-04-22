package fr.ailegalcase.document.vision;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Set;

/**
 * SF-148-01 : configuration de l'enrichissement visuel Claude Vision.
 *
 * <p>Stratégie hybride :
 * <ul>
 *   <li><b>Signal 1 (automatique)</b> — si une page a moins de
 *       {@link #minOcrCharsPerPage} caractères, vision est déclenchée quel que
 *       soit le type (cas : photo pure, page scannée illisible).</li>
 *   <li><b>Signal 2 (whitelist)</b> — {@link #triggerTypesByDomain} liste les
 *       types où vision apporte même avec OCR correct (SMS, ATTESTATION…).</li>
 *   <li><b>Blacklist</b> — {@link #skipTypesByDomain} : types où OCR suffit
 *       (CONTRAT, LETTRE, BULLETIN_PAIE…). Signal 1 prime sur la blacklist.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.vision")
public class VisionProperties {

    private boolean enabled = false;
    private int minOcrCharsPerPage = 40;
    private String model = "claude-haiku-4-5-20251001";
    private int maxTokens = 600;
    /** DPI utilisé pour rasteriser les pages PDF avant envoi à Claude Vision. */
    private int renderDpi = 150;

    private Map<String, Set<String>> triggerTypesByDomain = Map.of();
    private Map<String, Set<String>> skipTypesByDomain = Map.of();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMinOcrCharsPerPage() { return minOcrCharsPerPage; }
    public void setMinOcrCharsPerPage(int minOcrCharsPerPage) { this.minOcrCharsPerPage = minOcrCharsPerPage; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getRenderDpi() { return renderDpi; }
    public void setRenderDpi(int renderDpi) { this.renderDpi = renderDpi; }

    public Map<String, Set<String>> getTriggerTypesByDomain() { return triggerTypesByDomain; }
    public void setTriggerTypesByDomain(Map<String, Set<String>> v) { this.triggerTypesByDomain = v == null ? Map.of() : v; }

    public Map<String, Set<String>> getSkipTypesByDomain() { return skipTypesByDomain; }
    public void setSkipTypesByDomain(Map<String, Set<String>> v) { this.skipTypesByDomain = v == null ? Map.of() : v; }
}
