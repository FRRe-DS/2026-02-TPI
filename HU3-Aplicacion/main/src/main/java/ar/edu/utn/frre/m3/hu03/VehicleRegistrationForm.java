package ar.edu.utn.frre.m3.hu03;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VehicleRegistrationForm {
    @NotBlank(message = "La marca es obligatoria.")
    @Size(max = 60, message = "La marca no puede superar los 60 caracteres.")
    private String brand;

    @NotBlank(message = "El modelo es obligatorio.")
    @Size(max = 60, message = "El modelo no puede superar los 60 caracteres.")
    private String model;

    @NotNull(message = "El año es obligatorio.")
    @Min(value = 1886, message = "Ingresá un año válido.")
    @Max(value = 2100, message = "Ingresá un año válido.")
    private Integer year;

    @NotBlank(message = "La patente es obligatoria.")
    @Size(max = 20, message = "La patente no puede superar los 20 caracteres.")
    private String plate;

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
}
