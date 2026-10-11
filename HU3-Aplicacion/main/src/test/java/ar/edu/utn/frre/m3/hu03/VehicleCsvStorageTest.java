package ar.edu.utn.frre.m3.hu03;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VehicleCsvStorageTest {
    @TempDir Path temporaryFolder;

    @Test
    void appendsVehiclesAndSavesAllFieldsAndSelectedServices() throws Exception {
        VehicleCsvStorage storage = new VehicleCsvStorage(temporaryFolder);

        long firstNumber = storage.save("AUTO", form("Fiat", "Cronos", 2022, "AB 123 CD", "Blanco", 4,
                List.of("STANDARD", "PETS")));
        long secondNumber = storage.save("MOTO", form("Honda", "Wave", 2024, "A 123 BCD", "Negro", 2,
                List.of("DELIVERY")));

        assertEquals(1, firstNumber);
        assertEquals(2, secondNumber);
        String csv = Files.readString(temporaryFolder.resolve("vehiculos.csv"));
        assertTrue(csv.startsWith("numero,tipo,marca,modelo,anio,patente,color,capacidad,servicios"));
        assertTrue(csv.contains("1,\"AUTO\",\"Fiat\",\"Cronos\",2022,\"AB 123 CD\",\"Blanco\",4,\"STANDARD;PETS\""));
        assertTrue(csv.contains("2,\"MOTO\",\"Honda\",\"Wave\",2024,\"A 123 BCD\",\"Negro\",2,\"DELIVERY\""));
        assertEquals(3, csv.lines().count());
    }

    @Test
    void addsNewColumnsToExistingCsvAndPreservesPriorVehicleRows() throws Exception {
        Path csv = temporaryFolder.resolve("vehiculos.csv");
        Files.writeString(csv, "numero,tipo,marca,modelo,anio,patente\n"
                + "1,\"AUTO\",\"Fiat\",\"Cronos\",2022,\"AB 123 CD\"\n", StandardCharsets.UTF_8);
        VehicleCsvStorage storage = new VehicleCsvStorage(temporaryFolder);

        long newNumber = storage.save("MOTO", form("Honda", "Wave", 2024, "A 123 BCD", "Negro", 2,
                List.of("DELIVERY")));

        assertEquals(2, newNumber);
        String updatedCsv = Files.readString(csv);
        assertTrue(updatedCsv.contains("1,\"AUTO\",\"Fiat\",\"Cronos\",2022,\"AB 123 CD\",,,"));
        assertTrue(updatedCsv.contains("2,\"MOTO\",\"Honda\",\"Wave\",2024,\"A 123 BCD\",\"Negro\",2,\"DELIVERY\""));
    }

    private VehicleRegistrationForm form(String brand, String model, int year, String plate,
            String color, int capacity, List<String> services) {
        VehicleRegistrationForm form = new VehicleRegistrationForm();
        form.setBrand(brand);
        form.setModel(model);
        form.setYear(year);
        form.setPlate(plate);
        form.setColor(color);
        form.setCapacity(capacity);
        form.setServices(services);
        return form;
    }
}