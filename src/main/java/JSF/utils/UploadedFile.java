package JSF.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.http.Part;

/**
 * Fichier televerse par un formulaire {@code multipart/form-data}.
 *
 * <p>Comme pour le binding des champs simples, il suffit de declarer un
 * parametre de type {@code UploadedFile} dans la methode du controleur et de
 * le nommer avec {@code @RequestParam("photo")} (ou de compiler avec
 * {@code -parameters}) :</p>
 *
 * <pre>
 * &#64;UrlMapping(value = "/photo", method = "POST")
 * public ModelAndView envoyer(&#64;RequestParam("photo") UploadedFile photo) {
 *     System.out.println(photo.getSavedPath()); // chemin du ".txt" ecrit
 *     ...
 * }
 * </pre>
 *
 * <p>Au binding, le framework enregistre <strong>automatiquement</strong> le
 * contenu du fichier dans le dossier de stockage configure (paramètre
 * {@code uploadPath} du web.xml, par defaut {@code uploads}), sous forme de
 * ".txt" (voir {@link Uploads}). Le chemin ecrit est disponible via
 * {@link #getSavedPath()}. Un re-enregistrement reste possible avec
 * {@link #save()} ou {@link #saveTo(File)}.</p>
 */
public class UploadedFile {

    private final Part part;

    /** Fichier ".txt" ecrit par le framework au binding (null si non stocke). */
    private File savedFile;

    public UploadedFile(Part part) {
        if (part == null) {
            throw new IllegalArgumentException("Un UploadedFile ne peut pas etre construit a partir d'une part nulle.");
        }
        this.part = part;
    }

    /** Nom du fichier tel que envoye par le navigateur (peut etre null). */
    public String getOriginalFilename() {
        return part.getSubmittedFileName();
    }

    /** Type MIME declare par le navigateur (ex : "image/png"). */
    public String getContentType() {
        return part.getContentType();
    }

    /** Taille du fichier en octets. */
    public long getSize() {
        return part.getSize();
    }

    /** true si aucun fichier n'a ete selectionne (champ vide du formulaire). */
    public boolean isEmpty() {
        String name = getOriginalFilename();
        return name == null || name.isEmpty() || part.getSize() == 0;
    }

    /** Contenu complet du fichier en memoire. */
    public byte[] getBytes() throws IOException {
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        }
    }

    /** Flux de lecture du contenu du fichier (a fermer par l'appelant). */
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    /**
     * Enregistre le fichier dans le dossier de telechargement configure
     * (paramètre {@code uploadPath} du web.xml ou dossier {@code uploads}
     * par defaut), sous forme de ".txt".
     *
     * @return le fichier cree sur disque
     * @throws IOException si l'ecriture echoue
     */
    public File save() throws IOException {
        return Uploads.save(this);
    }

    /**
     * Enregistre le contenu du fichier dans le dossier donne, sous forme de
     * ".txt", avec un nom unique (le nom d'origine est assaini, un suffixe
     * est ajoute si besoin).
     *
     * @param directory dossier de destination (cree s'il n'existe pas)
     * @return le fichier cree sur disque
     * @throws IOException si l'ecriture echoue
     */
    public File saveTo(File directory) throws IOException {
        return Uploads.save(this, directory);
    }

    /** Fichier ".txt" ecrit par le framework au binding (null si non stocke). */
    public File getSavedFile() {
        return savedFile;
    }

    /** Chemin complet du ".txt" ecrit par le framework, null si rien n'a ete ecrit. */
    public String getSavedPath() {
        return savedFile == null ? null : savedFile.getAbsolutePath();
    }

    /** Nom du ".txt" ecrit par le framework, null si rien n'a ete ecrit. */
    public String getSavedName() {
        return savedFile == null ? null : savedFile.getName();
    }

    /** Appele par {@link Uploads} une fois le fichier ecrit sur disque. */
    void markSaved(File file) {
        this.savedFile = file;
    }

    /** L'objet {@code Part} du conteneur sous-jacent (accours aux API Servlet). */
    public Part getPart() {
        return part;
    }

    @Override
    public String toString() {
        return "UploadedFile[nom=" + getOriginalFilename() + ", type=" + getContentType()
                + ", taille=" + getSize() + "]";
    }
}
