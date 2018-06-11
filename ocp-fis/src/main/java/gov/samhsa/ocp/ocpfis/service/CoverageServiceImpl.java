package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.PeriodToPeriodDtoConverter;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class CoverageServiceImpl implements CoverageService {


    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    @Autowired
    public CoverageServiceImpl(IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService,FisProperties fisProperties){
        this.fhirClient=fhirClient;
        this.fhirValidator=fhirValidator;
        this.lookUpService=lookUpService;
        this.fisProperties=fisProperties;
    }

    @Override
    public void createCoverage(CoverageDto coverageDto) {
        Coverage coverage=convertCoverageDtoToCoverage(coverageDto);
        //Validate
        FhirUtil.validateFhirResource(fhirValidator,coverage, Optional.empty(), ResourceType.Coverage.name(),"Create Coverage");
        //Create
        FhirUtil.createFhirResource(fhirClient,coverage,ResourceType.Coverage.name());
    }


    private Coverage convertCoverageDtoToCoverage(CoverageDto coverageDto){
        Coverage coverage=new Coverage();
        try {
            coverage.setStatus(Coverage.CoverageStatus.fromCode(coverageDto.getStatus().getCode()));
        } catch (FHIRException e) {
            throw new ResourceNotFoundException("Status code not found");
        }
        coverage.setType(FhirDtoUtil.convertValuesetDtoToCodeableConcept(coverageDto.getType()));
        coverage.setSubscriber(FhirDtoUtil.mapReferenceDtoToReference(coverageDto.getSubscriber()));
        coverage.setSubscriberId(coverageDto.getSubscriberId());
        coverage.setBeneficiary(FhirDtoUtil.mapReferenceDtoToReference(coverageDto.getBeneficiary()));
        coverage.setRelationship(FhirDtoUtil.convertValuesetDtoToCodeableConcept(coverageDto.getRelationship()));

        Period period = new Period();
        period.setStart((coverageDto.getPeriod().getStart() != null) ? java.sql.Date.valueOf(coverageDto.getPeriod().getStart()) : null);
        period.setEnd((coverageDto.getPeriod().getEnd() != null) ? java.sql.Date.valueOf(coverageDto.getPeriod().getEnd()) : null);

        coverage.setPeriod(period);

        return coverage;
    }
}
