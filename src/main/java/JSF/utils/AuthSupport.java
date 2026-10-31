package JSF.utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

/**
 * Configuration et vérification de l'authentification par session.
 *
 * <p>Le nom de l'attribut de session qui matérialise l'utilisateur connecté est
 * lu dans le paramètre de contexte {@code authSessionKey} du {@code web.xml}.
 * Si ce paramètre est absent, la clé par défaut {@code utilisateur} est
 * utilisée.</p>
 */
public final class AuthSupport {

    public static final String INIT_PARAM = "authSessionKey";
    public static final String DEFAULT_SESSION_KEY = "utilisateur";

    private AuthSupport() {
    }

    /** Nom de l'attribut de session à contrôler. */
    public static String sessionKey(ServletContext context) {
        if (context == null) {
            return DEFAULT_SESSION_KEY;
        }
        String key = context.getInitParameter(INIT_PARAM);
        return (key == null || key.isBlank()) ? DEFAULT_SESSION_KEY : key.trim();
    }

    /**
     * Une session est authentifiée si l'attribut configuré existe et n'a pas
     * une valeur explicitement fausse ({@code false} ou chaîne vide).
     */
    public static boolean isAuthenticated(HttpSession session, String key) {
        if (session == null || key == null || key.isBlank()) {
            return false;
        }

        Object valeur = session.getAttribute(key);
        if (valeur == null) {
            return false;
        }
        if (valeur instanceof Boolean) {
            return (Boolean) valeur;
        }
        if (valeur instanceof String) {
            String texte = ((String) valeur).trim();
            return !texte.isEmpty() && !"false".equalsIgnoreCase(texte);
        }
        return true;
    }
}
