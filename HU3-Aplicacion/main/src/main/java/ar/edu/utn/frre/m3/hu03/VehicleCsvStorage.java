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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Stores the HU-03 vehicle fields in a local CSV file. */
@Service
public class VehicleCsvStorage {
    private static final String HEADER = "numero,tipo,marca,modelo,anio,patente,color,capacidad,servicios\n";
    private static final String OLD_HEADER = "numero,tipo,marca,modelo,anio,patente";

    private final Path storageFolder;

    @Autowired
    public VehicleCsvStorage(@Value("${hu03.storage.directory:}") String storageDirectory) {
        this(storageDirectory == null || storageDirectory.isBlank()
                ? Path.of("").toAbsolutePath().normalize()
                : Path.of(storageDirectory).toAbsolutePath().normalize());
    }

    VehicleCsvStorage(Path storageFolder) {
        this.storageFolder = storageFolder;
    }

    public synchronized long save(String type, VehicleRegistrationForm form) throws IOException {
        Files.createDirectories(storageFolder);
        Path csv = storageFolder.resolve("vehiculos.csv");
        ensureCurrentColumns(csv);
        long number = nextNumber(csv);
        String services = String.join(";", form.getServices());
        String row = number + "," + csv(type) + "," + csv(form.getBrand().trim()) + ","
                + csv(form.getModel().trim()) + "," + form.getYear() + "," + csv(form.getPlate().trim()) + ","
                + csv(form.getColor().trim()) + "," + form.getCapacity() + "," + csv(services) + "\n";
        appendCsv(csv, row);
        return number;
    }

    /** Adds the new columns to old application CSV files without losing existing vehicle data. */
    private void ensureCurrentColumns(Path csv) throws IOException {
        if (!Files.exists(csv) || Files.size(csv) == 0) return;
        String contents = Files.readString(csv, StandardCharsets.UTF_8);
        String[] lines = contents.split("\\R", -1);
        if (lines.length == 0 || lines[0].equals(HEADER.stripTrailing())) return;
        if (!lines[0].equals(OLD_HEADER)) {
            throw new IOException("El encabezado de vehiculos.csv no tiene el formato esperado.");
        }

        StringBuilder migrated = new StringBuilder(HEADER);
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].isEmpty()) migrated.append(lines[i]).append(",,,\n");
        }
        Path temporary = Files.createTempFile(storageFolder, ".vehiculos-", ".tmp");
        try {
            Files.writeString(temporary, migrated, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, csv, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, csv, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private long nextNumber(Path csv) throws IOException {
        long number = 1;
        if (Files.exists(csv)) {
            for (String line : Files.readAllLines(csv, StandardCharsets.UTF_8)) {
                int comma = line.indexOf(',');
                if (comma <= 0 || line.startsWith("numero,")) continue;
                try {
                    number = Math.max(number, Long.parseLong(line.substring(0, comma).replace("\"", "")) + 1);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed rows while preserving all valid prior IDs.
                }
            }
        }
        return number;
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
}