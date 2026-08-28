package local.camatt.dto;
import jakarta.validation.constraints.*;
public record RecognitionRequest(@NotNull Long employeeId, @DecimalMin("0") @DecimalMax("100") double confidence, String imagePath) {}

