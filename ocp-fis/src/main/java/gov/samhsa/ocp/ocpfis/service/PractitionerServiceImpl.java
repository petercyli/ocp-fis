package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.service.dto.PageDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerDto;
import gov.samhsa.ocp.ocpfis.service.dto.PractitionerRoleDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import gov.samhsa.ocp.ocpfis.service.exception.DuplicateResourceFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.PractitionerNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.PractitionerRoleNotFoundException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.web.PractitionerController;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Reference;
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
public class PractitionerServiceImpl implements PractitionerService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    @Autowired
    private LookUpService lookUpService;

    @Autowired
    public PractitionerServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
    }

    @Override
    public PageDto<PractitionerDto> getAllPractitioners(Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionersPerPage = ServiceUtil.getValidPageSize(size, ResourceType.Practitioner.name());
        boolean firstPage = true;


        IQuery practitionerIQuery = fhirClient.search().forResource(Practitioner.class);

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPagePractitionerBundle;
        Bundle otherPagePractitionerBundle;

        firstPagePractitionerBundle = (Bundle) practitionerIQuery
                .count(numberOfPractitionersPerPage)
                .returnBundle(Bundle.class)
                .execute();

        if (firstPagePractitionerBundle == null || firstPagePractitionerBundle.getEntry().size() < 1) {
            throw new PractitionerNotFoundException("No practitioners were found in the FHIR server");
        }

        otherPagePractitionerBundle = firstPagePractitionerBundle;

        if (page.isPresent() && page.get() > 1 && otherPagePractitionerBundle.getLink(Bundle.LINK_NEXT) != null) {
            // Load the required page
            firstPage = false;
            otherPagePractitionerBundle = ServiceUtil.getSearchBundleAfterFirstPage(firstPagePractitionerBundle, page.get(), numberOfPractitionersPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedPractitioners = otherPagePractitionerBundle.getEntry();

        return practitionersInPage(retrievedPractitioners, otherPagePractitionerBundle, numberOfPractitionersPerPage, firstPage, page);

    }


    @Override
    public PageDto<PractitionerDto> searchPractitioners(PractitionerController.SearchType type, String value, Optional<Boolean> showInactive, Optional<Integer> page, Optional<Integer> size) {
        int numberOfPractitionersPerPage = ServiceUtil.getValidPageSize(size, ResourceType.Practitioner.name());

        IQuery practitionerIQuery = fhirClient.search().forResource(Practitioner.class);
        boolean firstPage = true;

        if (type.equals(PractitionerController.SearchType.name))
            practitionerIQuery.where(new StringClientParam("name").matches().value(value.trim()));

        if (type.equals(PractitionerController.SearchType.identifier))
            practitionerIQuery.where(new TokenClientParam("identifier").exactly().code(value.trim()));

        if (showInactive.isPresent()) {
            if (!showInactive.get())
                practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        } else {
            practitionerIQuery.where(new TokenClientParam("active").exactly().code("true"));
        }

        Bundle firstPagePractitionerSearchBundle;
        Bundle otherPagePractitionerSearchBundle;

        firstPagePractitionerSearchBundle = (Bundle) practitionerIQuery.count(numberOfPractitionersPerPage).returnBundle(Bundle.class)
                .execute();

        if (firstPagePractitionerSearchBundle == null || firstPagePractitionerSearchBundle.isEmpty() || firstPagePractitionerSearchBundle.getEntry().size() < 1) {
            throw new PractitionerNotFoundException("No practitioners were found in the FHIR server.");
        }

        otherPagePractitionerSearchBundle = firstPagePractitionerSearchBundle;

        if (page.isPresent() && page.get() > 1 && otherPagePractitionerSearchBundle.getLink(Bundle.LINK_NEXT) != null) {
            firstPage = false;
            otherPagePractitionerSearchBundle = ServiceUtil.getSearchBundleAfterFirstPage(firstPagePractitionerSearchBundle, page.get(), numberOfPractitionersPerPage);
        }

        List<Bundle.BundleEntryComponent> retrievedPractitioners = otherPagePractitionerSearchBundle.getEntry();

        return practitionersInPage(retrievedPractitioners, otherPagePractitionerSearchBundle, numberOfPractitionersPerPage, firstPage, page);
    }


    public void createPractitioner(PractitionerDto practitionerDto) {
        //Check Duplicate Identifier
        boolean hasDuplicateIdentifier = practitionerDto.getIdentifiers().stream().anyMatch(identifierDto -> {
            if (fhirClient.search()
                    .forResource(Practitioner.class)
                    .where(new TokenClientParam("identifier")
                            .exactly().systemAndCode(identifierDto.getSystem(), identifierDto.getValue()))
                    .returnBundle(Bundle.class).execute().getTotal() > 0) {
                return true;
            }
            return false;
        });

        //When there is no duplicate identifier, practitioner gets created
        if (!hasDuplicateIdentifier) {
            //Create Fhir Practitioner
            Practitioner practitioner = modelMapper.map(practitionerDto, Practitioner.class);
            practitioner.setActive(true);

            final ValidationResult validationResult = fhirValidator.validateWithResult(practitioner);

            if (!validationResult.isSuccessful()) {
                throw new FHIRFormatErrorException("FHIR Practitioner Validation was not successful " + validationResult.getMessages());
            }

            MethodOutcome methodOutcome = fhirClient.create().resource(practitioner).execute();

            //Create PractitionerRole for the practitioner
            PractitionerRole practitionerRole = new PractitionerRole();
            practitionerDto.getPractitionerRoles().forEach(practitionerRoleCode -> {
                        //Assign fhir practitionerRole codes.
                        CodeableConcept codeableConcept = new CodeableConcept();
                        Coding coding = mapPractitionerRoleToCode(practitionerRoleCode);
                        List<Coding> codings = new ArrayList<>();
                        codings.add(coding);
                        codeableConcept.setCoding(codings);
                        practitionerRole.addCode(codeableConcept);
                    }
            );

            //Assign fhir Practitioner resource id.
            Reference practitionerId = new Reference();
            practitionerId.setReference("Practitioner/" + methodOutcome.getId().getIdPart());
            practitionerRole.setPractitioner(practitionerId);

            fhirClient.create().resource(practitionerRole).execute();

        } else {
            throw new DuplicateResourceFoundException("Practitioner with the same Identifier is already present.");
        }
    }

    @Override
    public void updatePractitioner(String practitionerId, PractitionerDto practitionerDto) {
        //Check Duplicate Identifier
        boolean hasDuplicateIdentifier = practitionerDto.getIdentifiers().stream().anyMatch(identifierDto -> {
            IQuery practitionersWithUpdatedIdentifierQuery = fhirClient.search()
                    .forResource(Practitioner.class)
                    .where(new TokenClientParam("identifier")
                            .exactly().systemAndCode(identifierDto.getSystem(), identifierDto.getValue()));
            Bundle practitionerWithUpdatedIdentifierBundle = (Bundle) practitionersWithUpdatedIdentifierQuery.returnBundle(Bundle.class).execute();
            Bundle practitionerWithUpdatedIdentifierAndSameResourceIdBundle = (Bundle) practitionersWithUpdatedIdentifierQuery.where(new TokenClientParam("_id").exactly().code(practitionerId)).returnBundle(Bundle.class).execute();
            if (practitionerWithUpdatedIdentifierBundle.getTotal() > 0) {
                if (practitionerWithUpdatedIdentifierBundle.getTotal() == practitionerWithUpdatedIdentifierAndSameResourceIdBundle.getTotal()) {
                    return false;
                } else {
                    return true;
                }
            }
            return false;
        });


        Practitioner existingPractitioner = fhirClient.read().resource(Practitioner.class).withId(practitionerId.trim()).execute();

        if (!hasDuplicateIdentifier) {
            Practitioner updatedpractitioner = modelMapper.map(practitionerDto, Practitioner.class);
            existingPractitioner.setIdentifier(updatedpractitioner.getIdentifier());
            existingPractitioner.setName(updatedpractitioner.getName());
            existingPractitioner.setTelecom(updatedpractitioner.getTelecom());
            existingPractitioner.setAddress(updatedpractitioner.getAddress());

            final ValidationResult validationResult = fhirValidator.validateWithResult(existingPractitioner);

            if (!validationResult.isSuccessful()) {
                throw new FHIRFormatErrorException("FHIR Practitioner Validation was not successful " + validationResult.getMessages());
            }

            MethodOutcome methodOutcome = fhirClient.update().resource(existingPractitioner)
                    .execute();

            //Update PractitionerRole for the practitioner
            PractitionerRole practitionerRole = new PractitionerRole();
            practitionerDto.getPractitionerRoles().forEach(practitionerRoleCode -> {
                        //Assign fhir practitionerRole codes.
                        CodeableConcept codeableConcept = new CodeableConcept();
                        Coding coding = mapPractitionerRoleToCode(practitionerRoleCode);
                        List<Coding> codings = new ArrayList<>();
                        codings.add(coding);
                        codeableConcept.setCoding(codings);
                        practitionerRole.addCode(codeableConcept);
                    }
            );

            //Assign fhir Practitioner resource id.
            Reference practitionerResourceId = new Reference();
            practitionerResourceId.setReference("Practitioner/" + methodOutcome.getId().getIdPart());

            practitionerRole.setPractitioner(practitionerResourceId);

            fhirClient.update().resource(practitionerRole).conditional()
                    .where(new ReferenceClientParam("practitioner")
                            .hasId("Practitioner/" + methodOutcome.getId().getIdPart())).execute();
        } else {
            throw new DuplicateResourceFoundException("Practitioner with the same Identifier is already present");
        }
    }

    @Override
    public PractitionerDto getPractitioner(String practitionerId) {
        Bundle practitionerBundle = fhirClient.search().forResource(Practitioner.class)
                .where(new TokenClientParam("_id").exactly().code(practitionerId))
                .returnBundle(Bundle.class)
                .execute();

        if (practitionerBundle == null || practitionerBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No practitioner was found for the givecn practitionerID:" + practitionerId);
        }

        Bundle.BundleEntryComponent retrievedPractitioner = practitionerBundle.getEntry().get(0);

        PractitionerDto practitionerDto = modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class);
        practitionerDto.setLogicalId(retrievedPractitioner.getResource().getIdElement().getIdPart());

        //Get Practitioner Role for the practitioner.
        List<PractitionerRoleDto> practitionerRoleDtos = getPractitionerRolesForEachPractitioner(retrievedPractitioner.getResource().getIdElement().getIdPart());
        practitionerDto.setPractitionerRoles(practitionerRoleDtos);

        return practitionerDto;
    }

    private PageDto<PractitionerDto> practitionersInPage(List<Bundle.BundleEntryComponent> retrievedPractitioners, Bundle otherPagePractitionerBundle, int numberOfPractitionersPerPage, boolean firstPage, Optional<Integer> page) {
        List<PractitionerDto> practitionersList = retrievedPractitioners.stream().map(retrievedPractitioner -> {
            PractitionerDto practitionerDto = modelMapper.map(retrievedPractitioner.getResource(), PractitionerDto.class);
            practitionerDto.setLogicalId(retrievedPractitioner.getResource().getIdElement().getIdPart());

            //Getting practitioner role into practitioner dto
            List<PractitionerRoleDto> practitionerRoleDtos = getPractitionerRolesForEachPractitioner(retrievedPractitioner.getResource().getIdElement().getIdPart());
            practitionerDto.setPractitionerRoles(practitionerRoleDtos);

            return practitionerDto;
        }).collect(Collectors.toList());

        double totalPages = Math.ceil((double) otherPagePractitionerBundle.getTotal() / numberOfPractitionersPerPage);
        int currentPage = firstPage ? 1 : page.get();

        return new PageDto<>(practitionersList, numberOfPractitionersPerPage, totalPages, currentPage, practitionersList.size(),
                otherPagePractitionerBundle.getTotal());
    }

    private List<PractitionerRoleDto> getPractitionerRolesForEachPractitioner(String practitionerId) {
        Bundle practitionerRoleBundle = (Bundle) fhirClient.search().forResource(PractitionerRole.class).where(new ReferenceClientParam("practitioner").hasId("Practitioner/" + practitionerId))
                .execute();
        List<PractitionerRoleDto> practitionerRoleDtos = new ArrayList<>();
        if (!practitionerRoleBundle.isEmpty() && practitionerRoleBundle.getEntry() != null && practitionerRoleBundle.getEntry().size() > 0) {
            PractitionerRole practitionerRole = (PractitionerRole) practitionerRoleBundle.getEntry().get(0).getResource();

            if (practitionerRole.getCode().size() > 0 && practitionerRole.getCode() != null) {
                practitionerRole.getCode().forEach(code -> {
                    PractitionerRoleDto practitionerRoleDto = new PractitionerRoleDto();
                    practitionerRoleDto.setCode((code.getCoding().size() > 0 || code.getCoding() != null) ? code.getCoding().get(0).getCode() : "");
                    practitionerRoleDto.setDisplay((code.getCoding().size() > 0 || code.getCoding() != null) ? code.getCoding().get(0).getDisplay() : "");
                    practitionerRoleDto.setSystem((code.getCoding().size() > 0 || code.getCoding() != null) ? code.getCoding().get(0).getSystem() : "");
                    practitionerRoleDtos.add(practitionerRoleDto);
                });
            }
        }
        return practitionerRoleDtos;
    }

    private Coding mapPractitionerRoleToCode(PractitionerRoleDto practitionerRoleDto) {
        ValueSetDto practitionerValueSet = getValueSetDtoByCode(practitionerRoleDto.getCode());
        return new Coding().setCode(practitionerValueSet.getCode()).setDisplay(practitionerValueSet.getDisplay()).setSystem(practitionerValueSet.getSystem());
    }

    private ValueSetDto getValueSetDtoByCode(String code) {
        List<ValueSetDto> practitionerRoleLookups = lookUpService.getPractitionerRoles();
        return practitionerRoleLookups.stream()
                .filter(practitionerRole -> practitionerRole.getCode().equalsIgnoreCase(code))
                .findAny()
                .orElseThrow(PractitionerRoleNotFoundException::new);
    }
}
