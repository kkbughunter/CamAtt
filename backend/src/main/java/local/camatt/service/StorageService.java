package local.camatt.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class StorageService {
    private final Path root;
    public StorageService(@Value("${camatt.storage-path}") String path) { this.root = Paths.get(path).toAbsolutePath().normalize(); }
    public List<StoredPhoto> saveEmployeePhotos(Long employeeId, List<MultipartFile> files) {
        try {
            Path folder = root.resolve("employees").resolve(employeeId.toString()).normalize();
            ensureInside(folder); Files.createDirectories(folder);
            List<StoredPhoto> saved = new ArrayList<>();
            for (MultipartFile file : files) {
                String type = file.getContentType();
                if (type == null || (!type.equals("image/jpeg") && !type.equals("image/png"))) throw new IllegalArgumentException("Photos must be JPG or PNG images");
                String extension = type.equals("image/png") ? ".png" : ".jpg";
                Path destination = folder.resolve(UUID.randomUUID() + extension).normalize(); ensureInside(destination);
                try (var input = file.getInputStream()) { Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING); }
                String relative = root.relativize(destination).toString().replace('\\', '/');
                saved.add(new StoredPhoto(destination, "/storage/" + relative, file.getOriginalFilename(), type));
            }
            return saved;
        } catch (IOException e) { throw new IllegalStateException("Could not store employee photos", e); }
    }
    public List<StoredPhoto> employeePhotos(Long employeeId) {
        Path folder = root.resolve("employees").resolve(employeeId.toString()).normalize();
        ensureInside(folder);
        if (!Files.isDirectory(folder)) return List.of();
        try (var paths = Files.list(folder)) {
            return paths.filter(Files::isRegularFile).sorted(Comparator.comparing(Path::toString)).map(path -> {
                String name = path.getFileName().toString();
                String type = name.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                String relative = root.relativize(path).toString().replace('\\', '/');
                return new StoredPhoto(path, "/storage/" + relative, name, type);
            }).toList();
        } catch (IOException e) { throw new IllegalStateException("Could not read stored employee photos", e); }
    }
    private void ensureInside(Path path) { if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid storage path"); }
    public Path root() { return root; }
    public record StoredPhoto(Path path, String publicUrl, String originalName, String contentType) {}
}
