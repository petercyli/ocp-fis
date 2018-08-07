package gov.samhsa.ocp.ocpfis.service.mapping;

import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import org.hl7.fhir.dstu3.model.Coverage;

import java.util.Optional;

public class CoverageToCoverageDtoMap {
    public static CoverageDto map(Coverage coverage) {
        CoverageDto coverageDto = new CoverageDto();
        coverageDto.setLogicalId(coverage.getIdElement().getIdPart());
        coverageDto.setStatus(coverage.getStatus().toCode());
        coverageDto.setStatusDisplay(Optional.of(coverage.getStatus().getDisplay()));
        coverage.getType().getCoding().stream().findAny().ifPresent(coding -> {
            coverageDto.setType(coding.getCode());
            coverageDto.setTypeDisplay(Optional.ofNullable(coding.getDisplay()));
        });

        coverageDto.setSubscriber(FhirDtoUtil.convertReferenceToReferenceDto(coverage.getSubscriber()));
        coverageDto.setSubscriberId(coverage.getSubscriberId());
        coverageDto.setBeneficiary(FhirDtoUtil.convertReferenceToReferenceDto(coverage.getBeneficiary()));
        coverage.getRelationship().getCoding().stream().findAny().ifPresent(coding -> {
            coverageDto.setRelationship(coding.getCode());
            coverageDto.setRelationshipDisplay(Optional.ofNullable(coding.getDisplay()));
        });

        coverageDto.setStartDate(DateUtil.convertDateToString(coverage.getPeriod().getStart()));
        coverageDto.setEndDate(DateUtil.convertDateToString(coverage.getPeriod().getEnd()));

        return coverageDto;
    }
}
