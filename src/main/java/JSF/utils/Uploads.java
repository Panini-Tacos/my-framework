package JSF.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class Uploads {

    public static final String INIT_PARAM = "uploadPath";
    public static final String DEFAULT_PATH = "uploads";
    public static final String STORAGE_EXTENSION = ".txt";

    private static volatile String configuredPath;

    private Uploads() {
    }

    // --- Configuration ---

    public static void configure(String path) {
        if (path != null && !path.trim().isEmpty()) {
            configuredPath = path.trim();
        }
    }

    public static String getUploadPath() {
        String configured = configuredPath;
        return configured != null ? configured : DEFAULT_PATH;
    }

    // --- Enregistrement ---

    public static File save(UploadedFile file) throws IOException {
        return save(file, new File(getUploadPath()));
    }

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

    // --- Noms de fichiers ---

    public static String sanitizeFilename(String name) {
        String base = name == null ? "" : name.replace('\0', ' ').trim();
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[\\\\/:*?\"<>|]", "_");
        base = base.replaceAll("\\s+", " ").trim();
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
        File[] fichiers = dossier.toFile().listFiles();
        if (fichiers != null) {
            for (File fichier : fichiers) {
                if (fichier.isFile()) {
                    dejaPris.add(fichier.getName());
                }
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
