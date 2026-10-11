package ar.edu.utn.frre.m3.hu03;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Stores only the HU-03 vehicle fields in CSV and copies the three uploaded documents. */
@Service
public class VehicleCsvStorage {
    private static final String HEADER = "numero,tipo,marca,modelo,anio,patente\n";
    private static final Map<String, String> DOCUMENT_NAMES = Map.of(
            "insurance", "seguro",
            "registration", "cedula",
            "license", "licencia");

    private final Path projectFolder;

    @Autowired
    public VehicleCsvStorage(@Value("${hu03.storage.directory:}") String storageDirectory) {
        this(storageDirectory == null || storageDirectory.isBlank()
                ? Path.of("").toAbsolutePath().normalize()
                : Path.of(storageDirectory).toAbsolutePath().normalize());
    }

    VehicleCsvStorage(Path projectFolder) {
        this.projectFolder = projectFolder;
    }

    public synchronized long save(String type, VehicleRegistrationForm form,
            MultipartFile insurance, MultipartFile registration, MultipartFile license) throws IOException {
        Path csv = projectFolder.resolve("vehiculos.csv");
        Path documentsRoot = projectFolder;
        Files.createDirectories(documentsRoot);

        long number = nextNumber(csv, documentsRoot);
        Path staging = Files.createTempDirectory(documentsRoot, ".vehiculo-" + number + "-");
        Path destination = documentsRoot.resolve("Vehiculo " + number);
        boolean csvWritten = false;
        try {
            copyDocument(staging, "insurance", insurance);
            copyDocument(staging, "registration", registration);
            copyDocument(staging, "license", license);
            moveWithoutOverwrite(staging, destination);

            String row = number + "," + csv(type) + "," + csv(form.getBrand().trim()) + ","
                    + csv(form.getModel().trim()) + "," + form.getYear() + "," + csv(form.getPlate().trim()) + "\n";
            appendCsv(csv, row);
            csvWritten = true;
            return number;
        } finally {
            if (!csvWritten) {
                deleteTree(destination);
                deleteTree(staging);
            }
        }
    }

    private long nextNumber(Path csv, Path documentsRoot) throws IOException {
        long number = 1;
        if (Files.exists(csv)) {
            for (String line : Files.readAllLines(csv, StandardCharsets.UTF_8)) {
                int comma = line.indexOf(',');
                if (comma <= 0 || line.startsWith("numero,")) continue;
                try {
                    number = Math.max(number, Long.parseLong(line.substring(0, comma).replace("\"", "")) + 1);
                } catch (NumberFormatException ignored) {
                    // Ignore an incomplete or malformed CSV row; valid prior IDs remain untouched.
                }
            }
        }
        while (Files.exists(documentsRoot.resolve("Vehiculo " + number))) number++;
        return number;
    }

    private void copyDocument(Path folder, String kind, MultipartFile file) throws IOException {
        String extension = allowedExtension(file);
        Path target = folder.resolve(DOCUMENT_NAMES.get(kind) + extension);
        try (var input = file.getInputStream()) {
            Files.copy(input, target);
        }
    }

    private String allowedExtension(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        int dot = original == null ? -1 : original.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("Cada documento debe ser PDF o una imagen admitida.");
        String extension = original.substring(dot).toLowerCase(Locale.ROOT);
        byte[] bytes;
        try (var input = file.getInputStream()) {
            bytes = input.readNBytes(12);
        }
        boolean valid = switch (extension) {
            case ".pdf" -> startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII));
            case ".png" -> startsWith(bytes, new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            case ".jpg", ".jpeg" -> bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case ".gif" -> startsWith(bytes, "GIF8".getBytes(StandardCharsets.US_ASCII));
            case ".webp" -> bytes.length >= 12 && startsWith(bytes, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP");
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("El archivo debe ser PDF, PNG, JPG/JPEG, GIF o WEBP válido.");
        return extension;
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) if (value[i] != prefix[i]) return false;
        return true;
    }

    private String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private void appendCsv(Path csv, String row) throws IOException {
        boolean hasHeader = Files.exists(csv) && Files.size(csv) > 0;
        String text = (hasHeader ? "" : HEADER) + row;
        try (FileChannel channel = FileChannel.open(csv, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            channel.lock();
            ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) channel.write(buffer);
        }
    }

    private void moveWithoutOverwrite(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, destination);
        }
    }

    private void deleteTree(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            for (Path item : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(item);
        }
    }
}
