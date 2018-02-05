package gov.samhsa.ocp.ocpfis.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.samhsa.ocp.ocpfis.config.FisProperties;
import gov.samhsa.ocp.ocpfis.service.dto.CareTeamDto;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRClientException;
import gov.samhsa.ocp.ocpfis.service.exception.FHIRFormatErrorException;
import gov.samhsa.ocp.ocpfis.service.exception.ResourceNotFoundException;
import gov.samhsa.ocp.ocpfis.service.mapping.CareTeamToCareTeamDtoConverter;
import gov.samhsa.ocp.ocpfis.service.mapping.dtotofhirmodel.CareTeamDtoToCareTeamConverter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CareTeam;
import org.hl7.fhir.exceptions.FHIRException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Service
@Slf4j
public class CareTeamServiceImpl implements CareTeamService {

    private final ModelMapper modelMapper;

    private final IGenericClient fhirClient;

    private final FhirValidator fhirValidator;

    private final FisProperties fisProperties;

    @Autowired
    public CareTeamServiceImpl(ModelMapper modelMapper, IGenericClient fhirClient, FhirValidator fhirValidator, FisProperties fisProperties) {
        this.modelMapper = modelMapper;
        this.fhirClient = fhirClient;
        this.fhirValidator = fhirValidator;
        this.fisProperties = fisProperties;

    }

    @Override
    public void createCareTeam(CareTeamDto careTeamDto) {
        checkForDuplicates(careTeamDto);

        try {
            final CareTeam careTeam = CareTeamDtoToCareTeamConverter.map(careTeamDto);

            validate(careTeam);

            fhirClient.create().resource(careTeam).execute();

        } catch (FHIRException | ParseException e) {
            throw new FHIRClientException("FHIR Client returned with an error while creating a care team:" + e.getMessage());

        }
    }

    @Override
    public void updateCareTeam(String careTeamId, CareTeamDto careTeamDto) {
        try {
            careTeamDto.setId(careTeamId);
            final CareTeam careTeam = CareTeamDtoToCareTeamConverter.map(careTeamDto);

            validate(careTeam);

            fhirClient.update().resource(careTeam).execute();

        } catch (FHIRException | ParseException e) {
            throw new FHIRClientException("FHIR Client returned with an error while creating a care team:" + e.getMessage());

        }
    }

    @Override
    public CareTeamDto getCareTeamById(String careTeamById) {
        Bundle careTeamBundle = fhirClient.search().forResource(CareTeam.class)
                .where(new TokenClientParam("_id").exactly().code(careTeamById))
                .returnBundle(Bundle.class)
                .execute();

        if(careTeamBundle == null || careTeamBundle.getEntry().size() < 1) {
            throw new ResourceNotFoundException("No CareTeam was found for the given careTeamID : " + careTeamById);
        }

        CareTeam careTeam = (CareTeam) careTeamBundle.getEntry().get(0).getResource();

        final CareTeamDto careTeamDto = CareTeamToCareTeamDtoConverter.map(careTeam);

        return careTeamDto;
    }

    private void checkForDuplicates(CareTeamDto careTeamDto) {

    }

    private void validate(CareTeam careTeam) {
        final ValidationResult validationResult = fhirValidator.validateWithResult(careTeam);

        if(!validationResult.isSuccessful()) {
            throw new FHIRFormatErrorException("FHIR CareTeam validation is not successful" + validationResult.getMessages());
        }
    }


}
