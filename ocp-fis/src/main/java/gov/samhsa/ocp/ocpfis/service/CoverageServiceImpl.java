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
import gov.samhsa.ocp.ocpfis.service.mapping.CoverageToCoverageDtoMap;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.CoverageDtoToCoverageMap;
import gov.samhsa.ocp.ocpfis.util.FhirProfileUtil;
import gov.samhsa.ocp.ocpfis.util.FhirUtil;
import gov.samhsa.ocp.ocpfis.util.PaginationUtil;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.ResourceType;
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

    private final RelatedPersonService relatedPersonService;

    @Autowired
    public CoverageServiceImpl(IGenericClient fhirClient, FhirValidator fhirValidator, LookUpService lookUpService, FisProperties fisProperties, ModelMapper modelMapper, RelatedPersonService relatedPersonService) {
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.lookUpService = lookUpService;
        this.fisProperties = fisProperties;
        this.modelMapper = modelMapper;
        this.relatedPersonService = relatedPersonService;
    }

    @Override
    public void createCoverage(CoverageDto coverageDto) {
        if (!isDuplicateWhileCreate(coverageDto)) {
            Coverage coverage = CoverageDtoToCoverageMap.map(coverageDto, lookUpService);

            //Set Profile Meta Data
            FhirProfileUtil.setCoverageProfileMetaData(fhirClient, coverage);
            //Validate
            FhirUtil.validateFhirResource(fhirValidator, coverage, Optional.empty(), ResourceType.Coverage.name(), "Create Coverage");
            //Create
            FhirUtil.createFhirResource(fhirClient, coverage, ResourceType.Coverage.name());
        } else {
            throw new DuplicateResourceFoundException("Coverage already exists for given subscriber id and beneficiary.");
        }
    }

    @Override
    public void updateCoverage(String id, CoverageDto coverageDto) {
        Coverage coverage = CoverageDtoToCoverageMap.map(coverageDto, lookUpService);
        coverage.setId(id);
        //Set Profile Meta Data
        FhirProfileUtil.setCoverageProfileMetaData(fhirClient, coverage);
        //Validate
        FhirUtil.validateFhirResource(fhirValidator, coverage, Optional.empty(), ResourceType.Coverage.name(), "Update Coverage");
        //Update
        FhirUtil.updateFhirResource(fhirClient, coverage, ResourceType.Coverage.name());
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
        int numberOfCoveragePerPage = PaginationUtil.getValidPageSize(fisProperties, pageSize, ResourceType.Coverage.name());
        Bundle firstPageCoverageBundle;
        Bundle otherPageCoverageBundle;
        boolean firstPage = true;

        //Getting list of coverages
        IQuery iQuery = FhirUtil.searchNoCache(fhirClient, Coverage.class, Optional.empty());

        iQuery.where(new ReferenceClientParam("beneficiary").hasId(patientId));


        firstPageCoverageBundle = PaginationUtil.getSearchBundleFirstPage(iQuery, numberOfCoveragePerPage, Optional.empty());

        if (firstPageCoverageBundle == null || firstPageCoverageBundle.getEntry().isEmpty()) {
            throw new ResourceNotFoundException("No Coverages were found in the FHIR server.");
        }

        otherPageCoverageBundle = firstPageCoverageBundle;

        if (pageNumber.isPresent() && pageNumber.get() > 1 && otherPageCoverageBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPageCoverageBundle = PaginationUtil.getSearchBundleAfterFirstPage(fhirClient, fisProperties, firstPageCoverageBundle, pageNumber.get(), numberOfCoveragePerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedCoverages = otherPageCoverageBundle.getEntry();

        List<CoverageDto> coverageDtos = retrievedCoverages.stream().map(cov -> {
            Coverage coverage = (Coverage) cov.getResource();
            return CoverageToCoverageDtoMap.map(coverage);
        }).collect(Collectors.toList());

        double totalPages = Math.ceil((double) otherPageCoverageBundle.getTotal() / numberOfCoveragePerPage);
        int currentPage = firstPage ? 1 : pageNumber.get();

        return new PageDto<>(coverageDtos, numberOfCoveragePerPage, totalPages, currentPage, coverageDtos.size(), otherPageCoverageBundle.getTotal());
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
