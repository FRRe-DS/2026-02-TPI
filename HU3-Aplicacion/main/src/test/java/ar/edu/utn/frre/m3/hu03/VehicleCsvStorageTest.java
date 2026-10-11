package ar.edu.utn.frre.m3.hu03;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class VehicleCsvStorageTest {
    @TempDir Path temporaryFolder;

    @Test
    void appendsVehiclesAndCopiesDocumentsIntoNumberedFolders() throws Exception {
        VehicleCsvStorage storage = new VehicleCsvStorage(temporaryFolder);
        VehicleRegistrationForm first = form("Fiat", "Cronos", 2022, "AB 123 CD");
        VehicleRegistrationForm second = form("Honda", "Wave", 2024, "A 123 BCD");

        long firstNumber = storage.save("AUTO", first, pdf("seguro"), png("cedula"), jpg("licencia"));
        long secondNumber = storage.save("MOTO", second, pdf("seguro"), png("cedula"), jpg("licencia"));

        assertEquals(1, firstNumber);
        assertEquals(2, secondNumber);
        String csv = Files.readString(temporaryFolder.resolve("vehiculos.csv"));
        assertTrue(csv.contains("1,\"AUTO\",\"Fiat\",\"Cronos\",2022,\"AB 123 CD\""));
        assertTrue(csv.contains("2,\"MOTO\",\"Honda\",\"Wave\",2024,\"A 123 BCD\""));
        assertEquals(3, csv.lines().count());
        assertTrue(Files.exists(temporaryFolder.resolve("Vehiculo 1/seguro.pdf")));
        assertTrue(Files.exists(temporaryFolder.resolve("Vehiculo 1/cedula.png")));
        assertTrue(Files.exists(temporaryFolder.resolve("Vehiculo 1/licencia.jpg")));
        assertTrue(Files.exists(temporaryFolder.resolve("Vehiculo 2/seguro.pdf")));
    }

    @Test
    void rejectsFileWithExtensionThatDoesNotMatchItsContents() throws Exception {
        VehicleCsvStorage storage = new VehicleCsvStorage(temporaryFolder);
        byte[] notPdf = "esto no es PDF".getBytes(StandardCharsets.UTF_8);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> storage.save("AUTO", form("Ford", "Ka", 2020, "AA 111 AA"),
                        new MockMultipartFile("insurance", "seguro.pdf", "application/pdf", notPdf),
                        png("cedula"), jpg("licencia")));
        assertFalse(Files.exists(temporaryFolder.resolve("vehiculos.csv")));
        try (var paths = Files.list(temporaryFolder)) {
            assertEquals(0, paths.count());
        }
    }

    private VehicleRegistrationForm form(String brand, String model, int year, String plate) {
        VehicleRegistrationForm form = new VehicleRegistrationForm();
        form.setBrand(brand);
        form.setModel(model);
        form.setYear(year);
        form.setPlate(plate);
        return form;
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile(name, name + ".pdf", "application/pdf", "%PDF-1.7 sample".getBytes(StandardCharsets.US_ASCII));
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile(name, name + ".png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1});
    }

    private MockMultipartFile jpg(String name) {
        return new MockMultipartFile(name, name + ".jpg", "image/jpeg", new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1});
    }
}
