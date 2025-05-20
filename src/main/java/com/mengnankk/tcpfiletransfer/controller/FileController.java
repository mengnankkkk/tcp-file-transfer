package com.mengnankk.tcpfiletransfer.controller;

import com.mengnankk.tcpfiletransfer.exception.StorageFileNotFoundException;
import com.mengnankk.tcpfiletransfer.factory.StorageServiceFactory;
import com.mengnankk.tcpfiletransfer.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.ui.Model;

@Controller
@RequestMapping("/file")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private final StorageService storageService;

    @Autowired
    public FileController(StorageServiceFactory storageServiceFactory) {
        this.storageService = storageServiceFactory.getStorageService();
    }

    @GetMapping("/")
    public String listUploadedFiles() {
        // Redirect to the upload form page by default
        return "redirect:/file/upload";
    }

    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        // The 'message' and 'error' attributes (if any from redirect) will be automatically added to the Model by Spring.
        // So, we just need to return the view name.
        return "uploadForm"; // This should match your HTML file name in templates folder (e.g., uploadForm.html)
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload.");
            redirectAttributes.addFlashAttribute("error", true);
            return "redirect:/file/upload";
        }
        try {
            String storedFilename = storageService.store(file);
            logger.info("File uploaded via web: {} as {}", file.getOriginalFilename(), storedFilename);
            redirectAttributes.addFlashAttribute("message", "File uploaded successfully: " + storedFilename);
        } catch (Exception e) {
            logger.error("Failed to upload file via web: {}", file.getOriginalFilename(), e);
            redirectAttributes.addFlashAttribute("message", "Failed to upload file: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", true);
        }
        return "redirect:/file/upload";
    }

    @GetMapping("/download/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        try {
            Resource file = storageService.loadAsResource(filename);
            if (file.exists() || file.isReadable()) {
                String encodedFilename = URLEncoder.encode(file.getFilename(), StandardCharsets.UTF_8.toString()).replace("+", "%20");
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                        .body(file);
            } else {
                logger.warn("Requested file not found or not readable for download: {}", filename);
                throw new StorageFileNotFoundException("Could not read file: " + filename);
            }
        } catch (StorageFileNotFoundException e) {
            logger.error("File not found for download: {}", filename, e);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            logger.error("Error during file download: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Optional: Exception handler for StorageFileNotFoundException within this controller
    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

    // A simple HTML form for testing upload (Optional - better to use a proper frontend)
    // To use this, create an uploadForm.html in src/main/resources/templates
    // The @GetMapping("/upload") now serves this purpose.
    // 日志语句已移动到相应方法内部
}