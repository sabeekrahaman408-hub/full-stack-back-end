package com.apollocare.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.apollocare.exception.BadRequestException;

import jakarta.annotation.PostConstruct;

/** Saves uploaded medical files on disk. The file type is verified from the file's real bytes, not its name. */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    public record StoredFile(String key, String originalName, String mimeType, long size) {
    }

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:./uploads/medical-records}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a file to upload");
        }
        String mime;
        String ext;
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (startsWith(head, 0x25, 0x50, 0x44, 0x46)) {                  // %PDF
                mime = "application/pdf";
                ext = "pdf";
            } else if (startsWith(head, 0xFF, 0xD8, 0xFF)) {
                mime = "image/jpeg";
                ext = "jpg";
            } else if (startsWith(head, 0x89, 0x50, 0x4E, 0x47)) {
                mime = "image/png";
                ext = "png";
            } else if (head.length >= 12 && startsWith(head, 0x52, 0x49, 0x46, 0x46)
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
                mime = "image/webp";
                ext = "webp";
            } else {
                throw new BadRequestException("Only PDF, JPG, PNG or WEBP files are allowed");
            }
        } catch (IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        String key = UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, resolve(key), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Could not save upload", e);
            throw new BadRequestException("The file could not be saved. Please try again.");
        }
        return new StoredFile(key, cleanName(file.getOriginalFilename(), ext), mime, file.getSize());
    }

    public Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new BadRequestException("Invalid file");
        }
        return p;
    }

    public void deleteQuietly(String key) {
        if (key == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(key));
        } catch (Exception e) {
            log.warn("Could not delete file {}: {}", key, e.getMessage());
        }
    }

    private static boolean startsWith(byte[] head, int... sig) {
        if (head.length < sig.length) {
            return false;
        }
        for (int i = 0; i < sig.length; i++) {
            if ((head[i] & 0xFF) != sig[i]) {
                return false;
            }
        }
        return true;
    }

    private static String cleanName(String original, String ext) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty()) {
            name = "medical-file." + ext;
        }
        return name.length() > 200 ? name.substring(name.length() - 200) : name;
    }
}
