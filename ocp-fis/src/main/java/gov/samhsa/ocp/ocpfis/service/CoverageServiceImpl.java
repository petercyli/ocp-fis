package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CoverageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PatientDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.util.DateUtil;
import gov.samhsa.ocp.ocpfis.util.FhirDtoUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
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

    @Override
    public PageDto<CoverageDto> getCoverages(String patientId, Optional<Integer> pageNumber, Optional<Integer> pageSize) {
        int numberOfCoveragePerPage= PaginationUtil.getValidPageSize(fisProperties,pageSize,ResourceType.Coverage.name());
        Bundle firstPageCoverageBundle;
        Bundle otherPageCoverageBundle;
        boolean firstPage=true;

        //Getting list of coverages
        IQuery iQuery=FhirUtil.searchNoCache(fhirClient,Coverage.class,Optional.empty());

        iQuery.where(new ReferenceClientParam("beneficiary").hasId(patientId));


        firstPageCoverageBundle=PaginationUtil.getSearchBundleFirstPage(iQuery,numberOfCoveragePerPage,Optional.empty());

        if(firstPageCoverageBundle ==null || firstPageCoverageBundle.getEntry().isEmpty()){
            throw new ResourceNotFoundException("No Coverages were found in the FHIR server.");
        }

        otherPageCoverageBundle = firstPageCoverageBundle;

        if(pageNumber.isPresent() && pageNumber.get() > 1 && otherPageCoverageBundle.getLink(Bundle.LINK_NEXT) !=null){
            firstPage=false;
            otherPageCoverageBundle=PaginationUtil.getSearchBundleAfterFirstPage(fhirClient,fisProperties,firstPageCoverageBundle,pageNumber.get(),numberOfCoveragePerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedCoverages=otherPageCoverageBundle.getEntry();

        List<CoverageDto> coverageDtos=retrievedCoverages.stream().map(cov-> {
            Coverage coverage = (Coverage) cov.getResource();
            return convertCoverageToCoverageDto(coverage);
        }).collect(Collectors.toList());

        double totalPages = Math.ceil((double) otherPageCoverageBundle.getTotal() / numberOfCoveragePerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(coverageDtos,numberOfCoveragePerPage,totalPages,currentPage,coverageDtos.size(),otherPageCoverageBundle.getTotal());
    }


    private Coverage convertCoverageDtoToCoverage(CoverageDto coverageDto) {
        Coverage coverage = new Coverage();
        try {
            coverage.setStatus(Coverage.CoverageStatus.fromCode(coverageDto.getStatus()));
        } catch (FHIRException e) {
            throw new ResourceNotFoundException("Status code not found");
        }
        coverage.setType(FhirDtoUtil.convertValuesetDtoToCodeableConcept(FhirDtoUtil.convertCodeToValueSetDto(coverageDto.getType(),lookUpService.getCoverageType())));
        coverage.setSubscriber(FhirDtoUtil.mapReferenceDtoToReference(coverageDto.getSubscriber()));
        coverage.setSubscriberId(coverageDto.getSubscriberId());
        coverage.setBeneficiary(FhirDtoUtil.mapReferenceDtoToReference(coverageDto.getBeneficiary()));
        coverage.setRelationship(FhirDtoUtil.convertValuesetDtoToCodeableConcept(FhirDtoUtil.convertCodeToValueSetDto(coverageDto.getRelationship(),lookUpService.getPolicyholderRelationship())));

        Period period = new Period();
        period.setStart((coverageDto.getStartDate() != null) ? java.sql.Date.valueOf(coverageDto.getStartDate()) : null);
        period.setEnd((coverageDto.getEndDate() != null) ? java.sql.Date.valueOf(coverageDto.getEndDate()) : null);

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

    private CoverageDto convertCoverageToCoverageDto(Coverage coverage){
        CoverageDto coverageDto=new CoverageDto();
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
        coverage.getRelationship().getCoding().stream().findAny().ifPresent(coding->{
            coverageDto.setRelationship(coding.getCode());
            coverageDto.setRelationshipDisplay(Optional.ofNullable(coding.getDisplay()));
        });

        coverageDto.setStartDate(DateUtil.convertDateToLocalDate(coverage.getPeriod().getStart()));
        coverageDto.setEndDate(DateUtil.convertDateToLocalDate(coverage.getPeriod().getEnd()));

        return coverageDto;
    }
}
