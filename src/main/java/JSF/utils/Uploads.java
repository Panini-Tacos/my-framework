package JSF.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration et enregistrement des fichiers televerses.
 *
 * <p>Le dossier de stockage est <strong>variable</strong> : il est defini par
 * le paramètre d'init {@code uploadPath} du {@code web.xml} (lu au demarrage
 * par {@code LiliFWContextListener}). S'il n'est pas defini, le dossier
 * {@code uploads} (relatif au working directory du conteneur) est utilise.</p>
 *
 * <pre>
 * &lt;context-param&gt;
 *     &lt;param-name&gt;uploadPath&lt;/param-name&gt;
 *     &lt;param-value&gt;/var/mon-app/documents&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 *
 * <p>Pour simplifier, les fichiers sont pour l'instant stockes sous forme de
 * ".txt" : le contenu est copie tel quel dans un fichier dont l'extension est
 * remplacee par {@code .txt} (ex : {@code photo.png} -> {@code photo.txt}).
 * Le nom est assaini et un suffixe {@code -1}, {@code -2}... evite
 * d'ecraser un fichier existant.</p>
 *
 * <p>L'enregistrement se fait automatiquement au binding : voir
 * {@link UploadedFile#getSavedPath()}.</p>
 */
public final class Uploads {

    /** Nom du paramètre d'init du web.xml (dossier de stockage variable). */
    public static final String INIT_PARAM = "uploadPath";

    /** Dossier utilise si aucun paramètre n'est configure. */
    public static final String DEFAULT_PATH = "uploads";

    /** Extension utilisee pour le stockage simplifie (texte). */
    public static final String STORAGE_EXTENSION = ".txt";

    private static volatile String configuredPath;

    private Uploads() {
    }

    /**
     * Fixe le dossier de stockage (appelle au demarrage avec la valeur du
     * paramètre {@code uploadPath} du web.xml).
     */
    public static void configure(String path) {
        if (path != null && !path.trim().isEmpty()) {
            configuredPath = path.trim();
        }
    }

    /** Dossier de stockage effectif (web.xml, sinon "uploads"). */
    public static String getUploadPath() {
        String configured = configuredPath;
        return configured != null ? configured : DEFAULT_PATH;
    }

    /**
     * Enregistre le fichier dans le dossier configure, avec un nom unique.
     *
     * @return le fichier cree sur disque
     */
    public static File save(UploadedFile file) throws IOException {
        return save(file, new File(getUploadPath()));
    }

    /**
     * Enregistre le contenu du fichier dans le dossier donne, sous forme de
     * ".txt". Le nom d'origine est assaini (plus de {@code ../}, de
     * separateurs ni de caracteres problematiques), son extension est
     * remplacee par {@code .txt} et, s'il existe deja, un suffixe
     * {@code -1}, {@code -2}... est ajoute pour ne jamais ecraser un fichier.
     *
     * @param directory dossier de destination (cree avec ses parents si besoin)
     * @return le fichier cree sur disque
     */
    public static File save(UploadedFile file, File directory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier a enregistrer (UploadedFile vide).");
        }
        Path dossier = directory.toPath();
        Files.createDirectories(dossier);

        Path cible = destinationUnique(dossier, asTxtFilename(sanitizeFilename(file.getOriginalFilename())));
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
        }
        file.markSaved(cible.toFile());
        return cible.toFile();
    }

    /**
     * Retire d'un nom de fichier tout ce qui pourrait sortir du dossier de
     * stockage (separateurs, {@code ..}, caracteres de controle) tout en
     * conservant une extension lisible.
     */
    public static String sanitizeFilename(String name) {
        String base = name == null ? "" : name.replace('\0', ' ').trim();
        // Ne garder que le dernier segment : "/etc/passwd" -> "passwd".
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        base = base.replaceAll("\\s+", " ").trim();
        // Pas de fichier cache ni de chemin relatif.
        while (base.startsWith(".")) {
            base = base.substring(1);
        }
        if (base.isEmpty()) {
            base = "fichier-" + System.currentTimeMillis();
        }
        if (base.length() > 200) {
            String ext = extension(base);
            base = base.substring(0, 200 - ext.length()) + ext;
        }
        return base;
    }

    /** "photo.png" -> "photo.txt" : extension remplacee par ".txt". */
    private static String asTxtFilename(String sanitized) {
        String ext = extension(sanitized);
        String base = ext.isEmpty() ? sanitized : sanitized.substring(0, sanitized.length() - ext.length());
        return base + STORAGE_EXTENSION;
    }

    private static Path destinationUnique(Path dossier, String nomFichier) throws IOException {
        Path candidat = dossier.resolve(nomFichier);
        if (!Files.exists(candidat)) {
            return candidat;
        }
        String racine = nomFichier.substring(0, nomFichier.length() - STORAGE_EXTENSION.length());
        List<String> dejaPris = new ArrayList<>();
        for (File fichier : dossier.toFile().listFiles()) {
            if (fichier.isFile()) {
                dejaPris.add(fichier.getName());
            }
        }
        for (int i = 1; ; i++) {
            candidat = dossier.resolve(racine + "-" + i + STORAGE_EXTENSION);
            if (!dejaPris.contains(candidat.getFileName().toString())) {
                return candidat;
            }
        }
    }

    private static String extension(String nomFichier) {
        int point = nomFichier.lastIndexOf('.');
        return point > 0 ? nomFichier.substring(point) : "";
    }
}
