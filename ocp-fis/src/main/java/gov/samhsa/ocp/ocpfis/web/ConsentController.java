package gov.samhsa.ocp.ocpfis.web;

import gov.samhsa.ocp.ocpfis.service.ConsentService;
import gov.samhsa.ocp.ocpfis.service.dto.ConsentDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PdfDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.pdf.ConsentPdfGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;

@RestController
public class ConsentController {
    @Autowired
    private ConsentService consentService;

    @Autowired
    private ConsentPdfGenerator consentPdfGenerator;

    @GetMapping("/consents")
    public PageDto<ConsentDto> getConsents(@RequestParam(value = "patient") Optional<String> patient,
                                           @RequestParam(value = "practitioner") Optional<String> practitioner,
                                           @RequestParam(value = "status") Optional<String> status,
                                           @RequestParam(value = "generalDesignation") Optional<Boolean> generalDesignation,
                                           @RequestParam Optional<Integer> pageNumber,
                                           @RequestParam Optional<Integer> pageSize) {
        return consentService.getConsents(patient, practitioner, status, generalDesignation, pageNumber, pageSize);
    }

    @GetMapping("/consents/{consentId}")
    public ConsentDto getAppointmentById(@PathVariable String consentId) {
        return consentService.getConsentsById(consentId);
    }

    @GetMapping("/consents/{consentId}/pdf")
    public PdfDto createPdf(@PathVariable String consentId) throws IOException {
        ConsentDto consentDto = consentService.getConsentsById(consentId);
        byte[] pdfBytes = consentPdfGenerator.generateConsentPdf(consentDto);

       // byte[] pdfBytes = consentPdfGenerator.generateConsentPdf(
        //        ConsentDto.builder()
        //                .logicalId("123123")
        //                .patient(ReferenceDto.builder().display("Test Patient").build())
        //                .build());
        return new PdfDto(pdfBytes);
    }
}
