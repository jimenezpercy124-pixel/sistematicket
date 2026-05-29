package com.siit.ticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FileServiceTest {

    private FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        fileService = new FileService();
        
        // Configurar la propiedad root de FileService para que apunte al directorio temporal
        java.lang.reflect.Field rootField = FileService.class.getDeclaredField("root");
        rootField.setAccessible(true);
        rootField.set(fileService, tempDir);
    }

    @Test
    public void testSaveAllowedFileSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "evidencia", 
                "reporte.pdf", 
                "application/pdf", 
                "Evidencia de prueba".getBytes()
        );

        String savedFilename = fileService.save(file);
        assertNotNull(savedFilename);
        assertTrue(savedFilename.contains("reporte.pdf"));
        assertTrue(Files.exists(tempDir.resolve(savedFilename)));
    }

    @Test
    public void testSaveForbiddenExtensionThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "evidencia", 
                "script.jsp", 
                "application/octet-stream", 
                "System.out.println(1);".getBytes()
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.save(file);
        });
        assertTrue(exception.getMessage().contains("Extensión de archivo no permitida"));
    }

    @Test
    public void testSaveLargeFileThrowsException() {
        // Crear contenido de más de 10 MB (11MB)
        byte[] largeBytes = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "evidencia", 
                "foto.png", 
                "image/png", 
                largeBytes
        );

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            fileService.save(file);
        });
        assertTrue(exception.getMessage().contains("excede el límite de tamaño de 10MB"));
    }

    @Test
    public void testGetFilePathSafe() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "evidencia", 
                "foto.jpg", 
                "image/jpeg", 
                "jpgcontent".getBytes()
        );

        String filename = fileService.save(file);
        Path resolvedPath = fileService.getFilePath(filename);
        assertNotNull(resolvedPath);
        assertEquals(tempDir.resolve(filename).toAbsolutePath().normalize(), resolvedPath.toAbsolutePath().normalize());
    }

    @Test
    public void testGetFilePathPathTraversalThrowsException() {
        assertThrows(SecurityException.class, () -> {
            fileService.getFilePath("../malicioso.txt");
        });
        
        assertThrows(SecurityException.class, () -> {
            fileService.getFilePath("subfolder/../../malicioso.txt");
        });
        
        assertThrows(SecurityException.class, () -> {
            fileService.getFilePath("c:/archivo.txt");
        });
    }
}
