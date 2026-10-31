package JSF.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.http.Part;

public class UploadedFile {

    private final Part part;
    private File savedFile;

    public UploadedFile(Part part) {
        if (part == null) {
            throw new IllegalArgumentException("Un UploadedFile ne peut pas etre construit a partir d'une part nulle.");
        }
        this.part = part;
    }

    public String getOriginalFilename() {
        return part.getSubmittedFileName();
    }

    public String getContentType() {
        return part.getContentType();
    }

    public long getSize() {
        return part.getSize();
    }

    public boolean isEmpty() {
        String name = getOriginalFilename();
        return name == null || name.isEmpty() || part.getSize() == 0;
    }

    public byte[] getBytes() throws IOException {
        try (InputStream in = part.getInputStream()) {
            return in.readAllBytes();
        }
    }

    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    public File save() throws IOException {
        return Uploads.save(this);
    }

    public File saveTo(File directory) throws IOException {
        return Uploads.save(this, directory);
    }

    public File getSavedFile() {
        return savedFile;
    }

    public String getSavedPath() {
        return savedFile == null ? null : savedFile.getAbsolutePath();
    }

    public String getSavedName() {
        return savedFile == null ? null : savedFile.getName();
    }

    void markSaved(File file) {
        this.savedFile = file;
    }

    public Part getPart() {
        return part;
    }

    @Override
    public String toString() {
        return "UploadedFile[nom=" + getOriginalFilename() + ", type=" + getContentType()
                + ", taille=" + getSize() + "]";
    }
}
