package gov.samhsa.ocp.ocpfis.data.model.task;

import gov.samhsa.ocp.ocpfis.data.model.activitydefinition.TempPeriodDto;
import gov.samhsa.ocp.ocpfis.service.dto.PeriodDto;
import gov.samhsa.ocp.ocpfis.service.dto.ReferenceDto;
import gov.samhsa.ocp.ocpfis.service.dto.ValueSetDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempTaskDto {
    private String logicalId;

    //Reference to activity definition
    private ReferenceDto definition;

    //Parent task
    private ReferenceDto partOf;

    /*Task Status.
    *Eg:draft, requested, received, accepted*/
    private ValueSetDto status;

    /*Proposal intent.
    * Eg:proposal,plan,order */
    private ValueSetDto intent;

    /*Request Priority.
      *Eg: normal, urgent, asap, stat */
    private ValueSetDto priority;

    private String description;

    //Patient who benefits from the task
    private ReferenceDto beneficiary;

    /*Health care event during the task creation.
      *Assign episodeOf care when ActivityDefinition name="enrollment" */
    private ReferenceDto context;

    /*Start and end time of execution.
     * Start date when status is changed to "in-progress"
     * End date when status is changed to "Completed"*/
    private TempPeriodDto executionPeriod;

    //Task Creation Date
    private String authoredOn;


    private String lastModified;

    //Creator Practitioner of the task
    private ReferenceDto agent;

    /*TaskPerformerType.
    Eg:requester | dispatcher | scheduler | performer | monitor | manager | acquirer | reviewer */
    private ValueSetDto performerType;

    //Practitioner who perform the task
    private ReferenceDto owner;

    /*
    Comments about task. Generally entered by the owner who is assigned to complete the task
     */
    private String note;

    //managingOrganization - Organization the agent is acting for
    private ReferenceDto organization;
}
