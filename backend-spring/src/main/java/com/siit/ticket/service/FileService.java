package com.siit.ticket.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileService {

    private final Path root = Paths.get("uploads");

    private final java.util.Set<String> ALLOWED_EXTENSIONS = java.util.Set.of("pdf", "png", "jpg", "jpeg", "docx", "xlsx");

    public FileService() {
        try {
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar la carpeta de subidas");
        }
    }

    public String save(MultipartFile file) {
        try {
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new IllegalArgumentException("El archivo excede el límite de tamaño de 10MB.");
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.contains(".")) {
                throw new IllegalArgumentException("El archivo no tiene una extensión válida.");
            }

            String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException("Extensión de archivo no permitida. Solo se permiten: " + ALLOWED_EXTENSIONS);
            }

            String cleanOriginalName = originalName.replace(",", "_");
            String filename = UUID.randomUUID().toString() + "_" + cleanOriginalName;
            Files.copy(file.getInputStream(), this.root.resolve(filename));
            return filename;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar el archivo: " + e.getMessage());
        }
    }

    public Path getFilePath(String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new SecurityException("Nombre de archivo inválido o intento de path traversal detectado.");
        }
        Path rootAbs = this.root.toAbsolutePath().normalize();
        Path resolvedPath = this.root.resolve(filename).toAbsolutePath().normalize();
        
        if (!resolvedPath.startsWith(rootAbs)) {
            throw new SecurityException("Acceso no autorizado fuera de la carpeta de subidas.");
        }
        if (!Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
            throw new RuntimeException("El archivo solicitado no existe.");
        }
        return resolvedPath;
    }
}
