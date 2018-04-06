package gov.samhsa.ocp.ocpfis.service.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class PdfDto {
    private byte[] pdfBytes;

    public PdfDto(byte[] pdfBytes) {
        this.pdfBytes = pdfBytes;
    }
}
