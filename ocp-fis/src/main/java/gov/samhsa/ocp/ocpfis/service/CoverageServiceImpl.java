package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.ResourceType;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CoverageServiceImpl implements CoverageService {


    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final LookUpService lookUpService;

    private final FisProperties fisProperties;

    private final ModelMapper modelMapper;

    @Autowired
    private RelatedPersonService relatedPersonService;

    @Autowired
    public CoverageServiceImpl(IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties, ModelMapper modelMapper) {
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
        this.modelMapper = modelMapper;
    }

    @Override
    public void createCoverage(CoverageDto coverageDto) {
        if (!isDuplicateWhileCreate(coverageDto)) {
            Coverage coverage = convertCoverageDtoToCoverage(coverageDto);
            //Validate
            FhirUtil.validateFhirResource(fhirValidator, coverage, Optional.empty(), ResourceType.Coverage.name(), "Create Coverage");
            //Create
            FhirUtil.createFhirResource(fhirClient, coverage, ResourceType.Coverage.name());
        } else {
            throw new DuplicateResourceFoundException("Coverage already exists for given subscriber id and beneficiary.");
        }
    }

    @Override
    public List<ReferenceDto> getSubscriberOptions(String patientId) {
        List<ReferenceDto> referenceDtoList = new ArrayList<>();

        Patient patient = fhirClient.read().resource(Patient.class).withId(patientId).execute();

        ReferenceDto patientReference = new ReferenceDto();
        patientReference.setReference(ResourceType.Patient + "/" + patientId);
        patientReference.setDisplay(modelMapper.map(patient, PatientDto.class).getName().stream().findAny().get().getFirstName() + " " + modelMapper.map(patient, PatientDto.class).getName().stream().findAny().get().getLastName());
        referenceDtoList.add(patientReference);

        referenceDtoList.addAll(relatedPersonService.searchRelatedPersons(patientId, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(true)).getElements().stream()
                .map(relPer -> {
                    ReferenceDto relatedPersonReference = new ReferenceDto();
                    relatedPersonReference.setDisplay(relPer.getFirstName() + " " + relPer.getLastName());
                    relatedPersonReference.setReference(ResourceType.RelatedPerson + "/" + relPer.getRelatedPersonId());
                    return relatedPersonReference;
                }).collect(Collectors.toList()));

        return referenceDtoList;
    }


    private Coverage convertCoverageDtoToCoverage(CoverageDto coverageDto) {
        Coverage coverage = new Coverage();
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

    private boolean isDuplicateWhileCreate(CoverageDto coverageDto) {
        Bundle bundle = (Bundle) FhirUtil.setNoCacheControlDirective(fhirClient.search().forResource(Coverage.class)
                .where(new ReferenceClientParam("beneficiary").hasId(coverageDto.getBeneficiary().getReference())))
                .returnBundle(Bundle.class).execute();
        return !bundle.getEntry().stream().map(bundleEntryComponent -> {
            Coverage coverage = (Coverage) bundleEntryComponent.getResource();
            return coverage.getSubscriberId();
        }).filter(id -> id.equalsIgnoreCase(coverageDto.getSubscriberId().trim())).collect(Collectors.toList()).isEmpty();

    }
}
