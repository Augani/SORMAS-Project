package de.symeda.sormas.rest;

import java.util.Date;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.PushResult;
import de.symeda.sormas.api.therapy.TreatmentDto;
import de.symeda.sormas.api.user.UserReferenceDto;

@Path("/treatments")
@Produces({ MediaType.APPLICATION_JSON + "; charset=UTF-8" })
@Consumes({ MediaType.APPLICATION_JSON + "; charset=UTF-8" })
@RolesAllowed("USER")
public class TreatmentResource extends EntityDtoResource {

	@GET
	@Path("/all/{since}")
	public List<TreatmentDto> getAllTreatments(@Context SecurityContext sc, @PathParam("since") long since) {
		UserReferenceDto userDto = FacadeProvider.getUserFacade().getByUserNameAsReference(sc.getUserPrincipal().getName());
		return FacadeProvider.getTreatmentFacade().getAllActiveTreatmentsAfter(new Date(since), userDto.getUuid());
	}

	@POST
	@Path("/query")
	public List<TreatmentDto> getByUuids(@Context SecurityContext sc, List<String> uuids) {
		return FacadeProvider.getTreatmentFacade().getByUuids(uuids);
	}

	@POST
	@Path("/push")
	public List<PushResult> postTreatments(List<TreatmentDto> dtos) {
		return savePushedDto(dtos, FacadeProvider.getTreatmentFacade()::saveTreatment);
	}

	@GET
	@Path("/uuids")
	public List<String> getAllActiveUuids(@Context SecurityContext sc) {
		UserReferenceDto userDto = FacadeProvider.getUserFacade()
				.getByUserNameAsReference(sc.getUserPrincipal().getName());	
		return FacadeProvider.getTreatmentFacade().getAllActiveUuids(userDto.getUuid());
	}

}