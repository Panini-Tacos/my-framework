package JSF.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

public final class AuthSupport {

    public static final String INIT_PARAM = "authSessionKey";
    public static final String DEFAULT_SESSION_KEY = "utilisateur";
    public static final String ROLE_INIT_PARAM = "roleSessionKey";
    public static final String DEFAULT_ROLE_KEY = "role";

    private AuthSupport() {
    }

    // --- Configuration ---

    public static String sessionKey(ServletContext context) {
        return valeurOuDefaut(context, INIT_PARAM, DEFAULT_SESSION_KEY);
    }

    public static String roleSessionKey(ServletContext context) {
        return valeurOuDefaut(context, ROLE_INIT_PARAM, DEFAULT_ROLE_KEY);
    }

    // --- Authentification ---

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

    // --- Rôles ---

    public static boolean hasAnyRole(HttpSession session, String roleKey, String[] rolesDemandes) {
        if (session == null || roleKey == null || roleKey.isBlank()
                || rolesDemandes == null || rolesDemandes.length == 0) {
            return false;
        }

        List<String> rolesSession = extraireRoles(session.getAttribute(roleKey));
        if (rolesSession.isEmpty()) {
            return false;
        }

        for (String roleDemande : rolesDemandes) {
            if (roleDemande == null || roleDemande.isBlank()) {
                continue;
            }
            String attendu = roleDemande.trim();
            for (String roleSession : rolesSession) {
                if (attendu.equalsIgnoreCase(roleSession)) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Outils ---

    private static String valeurOuDefaut(ServletContext context, String nom, String defaut) {
        if (context == null) {
            return defaut;
        }
        String valeur = context.getInitParameter(nom);
        return (valeur == null || valeur.isBlank()) ? defaut : valeur.trim();
    }

    private static List<String> extraireRoles(Object valeur) {
        List<String> roles = new ArrayList<>();
        if (valeur == null) {
            return roles;
        }
        if (valeur instanceof Collection<?>) {
            for (Object element : (Collection<?>) valeur) {
                ajouterRoles(roles, element);
            }
        } else if (valeur.getClass().isArray()) {
            int taille = Array.getLength(valeur);
            for (int i = 0; i < taille; i++) {
                ajouterRoles(roles, Array.get(valeur, i));
            }
        } else {
            ajouterRoles(roles, valeur);
        }
        return roles;
    }

    private static void ajouterRoles(List<String> roles, Object valeur) {
        if (valeur == null) {
            return;
        }
        for (String morceau : String.valueOf(valeur).split(",")) {
            String role = morceau.trim();
            if (!role.isEmpty()) {
                roles.add(role);
            }
        }
    }
}
