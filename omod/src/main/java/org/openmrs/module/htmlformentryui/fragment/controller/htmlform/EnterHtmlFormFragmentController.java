/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.htmlformentryui.fragment.controller.htmlform;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateMidnight;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.FormService;
import org.openmrs.api.ValidationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.appframework.feature.FeatureToggleProperties;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.adt.exception.EncounterDateAfterVisitStopDateException;
import org.openmrs.module.emrapi.adt.exception.EncounterDateBeforeVisitStartDateException;
import org.openmrs.module.emrapi.encounter.EncounterDomainWrapper;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentryui.HtmlFormUtil;
import org.openmrs.module.uicommons.UiCommonsConstants;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentConfiguration;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.ui.framework.resource.ResourceFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class EnterHtmlFormFragmentController extends BaseHtmlFormFragmentController {
	
	private static final Log log = LogFactory.getLog(EnterHtmlFormFragmentController.class);
	
	/**
	 * @param config
	 * @param sessionContext
	 * @param htmlFormEntryService
	 * @param formService
	 * @param resourceFactory
	 * @param patient
	 * @param hf
	 * @param form
	 * @param formUuid
	 * @param definitionUiResource
	 * @param encounter
	 * @param visit
	 * @param returnUrl
	 * @param automaticValidation defaults to true. If you don't want HFE's automatic validation, set it
	 *            to false
	 * @param model
	 * @param httpSession
	 * @throws Exception
	 */
	public void controller(FragmentConfiguration config, UiSessionContext sessionContext, UiUtils ui,
	        @SpringBean("htmlFormEntryService") HtmlFormEntryService htmlFormEntryService,
	        @SpringBean("adtService") AdtService adtService, @SpringBean("formService") FormService formService,
	        @SpringBean("coreResourceFactory") ResourceFactory resourceFactory,
	        @SpringBean("featureToggles") FeatureToggleProperties featureToggles, @FragmentParam("patient") Patient patient,
	        @FragmentParam(value = "htmlForm", required = false) HtmlForm hf,
	        @FragmentParam(value = "htmlFormId", required = false) Integer htmlFormId,
	        @FragmentParam(value = "formId", required = false) Form form,
	        @FragmentParam(value = "formUuid", required = false) String formUuid,
	        @FragmentParam(value = "definitionUiResource", required = false) String definitionUiResource,
	        @FragmentParam(value = "encounter", required = false) Encounter encounter,
	        @FragmentParam(value = "encounterDate", required = false) Date encounterDate, // allows specifying a default encounter date when adding a new encounter; should not be used with encounter param
	        @FragmentParam(value = "visit", required = false) Visit visit,
	        @FragmentParam(value = "createVisit", required = false) Boolean createVisit,
	        @FragmentParam(value = "returnUrl", required = false) String returnUrl,
	        @FragmentParam(value = "automaticValidation", defaultValue = "true") boolean automaticValidation,
	        FragmentModel model, HttpSession httpSession) throws Exception {
		
		config.require("patient", "htmlForm | htmlFormId | formId | formUuid | definitionUiResource | encounter");
		
		if (hf == null) {
			if (htmlFormId != null) {
				hf = htmlFormEntryService.getHtmlForm(htmlFormId);
			} else if (form != null) {
				hf = htmlFormEntryService.getHtmlFormByForm(form);
			} else if (formUuid != null) {
				form = formService.getFormByUuid(formUuid);
				hf = htmlFormEntryService.getHtmlFormByForm(form);
			} else if (StringUtils.isNotBlank(definitionUiResource)) {
				hf = HtmlFormUtil.getHtmlFormFromUiResource(resourceFactory, formService, htmlFormEntryService,
				    definitionUiResource, encounter);
			}
		}
		if (hf == null && encounter != null) {
			form = encounter.getForm();
			if (form == null) {
				throw new IllegalArgumentException("Cannot view a form-less encounter unless you specify which form to use");
			}
			hf = HtmlFormEntryUtil.getService().getHtmlFormByForm(encounter.getForm());
			if (hf == null)
				throw new IllegalArgumentException("The form for the specified encounter (" + encounter.getForm()
				        + ") does not have an HtmlForm associated with it");
		}
		if (hf == null)
			throw new RuntimeException("Could not find HTML Form");
		
		// the code below doesn't handle the HFFS case where you might want to _add_ data to an existing encounter
		FormEntrySession fes;
		if (encounter != null) {
			fes = new FormEntrySession(patient, encounter, FormEntryContext.Mode.EDIT, hf, null, httpSession,
			        automaticValidation, !automaticValidation);
		} else {
			fes = new FormEntrySession(patient, hf, FormEntryContext.Mode.ENTER, null, httpSession, automaticValidation,
			        !automaticValidation);
		}
		
		String visitStartDatetime = null;
		String visitStopDatetime = null;
		
		//If GP timezone is true, it will convert the visitStartDatetime and visitStopDatetime to UTC and format RFC3339
		if (visit != null) {
			if (visit.getStartDatetime() != null) {
				visitStartDatetime = ui.dateToISOString(visit.getStartDatetime());
			}
			if (visit.getStopDatetime() != null) {
				visitStopDatetime = ui.dateToISOString(visit.getStopDatetime());
			} else {
				visitStopDatetime = ui.dateToISOString(new Date());
			}
		}
		
		// use defaultEncounterDate if specified, otherwise if retro entry (visit is closed) use visit start date, otherwise null
		Date defaultEncounterDate = encounterDate != null ? encounterDate
		        : ((visit != null && visit.getStopDatetime() != null) ? visit.getStartDatetime() : null);
		
		VisitDomainWrapper visitDomainWrapper = getVisitDomainWrapper(visit, encounter, adtService);
		setupVelocityContext(fes, visitDomainWrapper, ui, sessionContext, featureToggles);
		setupFormEntrySession(fes, visitDomainWrapper, defaultEncounterDate, ui, sessionContext, returnUrl);
		setupModel(model, fes, visitDomainWrapper, createVisit, visitStartDatetime, visitStopDatetime, ui);
		
	}
	
	/**
	 * Creates a simple object to record if there is an authenticated user
	 * 
	 * @return the simple object
	 */
	public SimpleObject checkIfLoggedIn() {
		return SimpleObject.create("isLoggedIn", Context.isAuthenticated());
	}
	
	/**
	 * Tries to authenticate with the given credentials
	 * 
	 * @param user the username
	 * @param pass the password
	 * @return a simple object to record if successful
	 */
	public SimpleObject authenticate(@RequestParam("user") String user, @RequestParam("pass") String pass,
	        UiSessionContext context, @SpringBean EmrApiProperties emrApiProperties) {
		try {
			Context.authenticate(user, pass);
			context.setSessionLocation(emrApiProperties.getUnknownLocation());
		}
		catch (ContextAuthenticationException ex) {
			
		}
		
		return checkIfLoggedIn();
	}
	
	/**
	 * Handles a form submit request
	 * 
	 * @param patient
	 * @param hf
	 * @param encounter
	 * @param visit
	 * @param returnUrl
	 * @param request
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public SimpleObject submit(UiSessionContext sessionContext, @RequestParam("personId") Patient patient,
	        @RequestParam("htmlFormId") HtmlForm hf,
	        @RequestParam(value = "encounterId", required = false) Encounter encounter,
	        @RequestParam(value = "visitId", required = false) Visit visit,
	        @RequestParam(value = "createVisit", required = false) Boolean createVisit,
	        @RequestParam(value = "returnUrl", required = false) String returnUrl,
	        @SpringBean("adtService") AdtService adtService,
	        @SpringBean("featureToggles") FeatureToggleProperties featureToggles, UiUtils ui, HttpServletRequest request)
	        throws Exception {
		
		// TODO formModifiedTimestamp and encounterModifiedTimestamp
		
		boolean editMode = encounter != null;
		
		FormEntrySession fes;
		if (encounter != null) {
			fes = new FormEntrySession(patient, encounter, FormEntryContext.Mode.EDIT, hf, request.getSession());
		} else {
			fes = new FormEntrySession(patient, hf, FormEntryContext.Mode.ENTER, request.getSession());
		}
		
		VisitDomainWrapper visitDomainWrapper = getVisitDomainWrapper(visit, encounter, adtService);
		setupVelocityContext(fes, visitDomainWrapper, ui, sessionContext, featureToggles);
		setupFormEntrySession(fes, visitDomainWrapper, null, ui, sessionContext, returnUrl);
		fes.getHtmlToDisplay(); // needs to happen before we validate or process a form
		
		// Validate and return with errors if any are found
		List<FormSubmissionError> validationErrors = fes.getSubmissionController().validateSubmission(fes.getContext(),
		    request);
		if (validationErrors.size() > 0) {
			return returnHelper(validationErrors, fes, null);
		}
		
		try {
			// No validation errors found so process form submission
			fes.prepareForSubmit();
			fes.getSubmissionController().handleFormSubmission(fes, request);
		}
		catch (Exception ex) {
			StringWriter sw = new StringWriter();
			ex.printStackTrace(new PrintWriter(sw));
			validationErrors.add(new FormSubmissionError("general-form-error",
			        "Form submission error " + ex.getMessage() + "<br/>" + sw.toString()));
			return returnHelper(validationErrors, fes, null);
		}
		
		// Check this form will actually create an encounter if its supposed to
		if (fes.getContext().getMode() == FormEntryContext.Mode.ENTER && fes.hasEncouterTag()
		        && (fes.getSubmissionActions().getEncountersToCreate() == null
		                || fes.getSubmissionActions().getEncountersToCreate().size() == 0)) {
			throw new IllegalArgumentException("This form is not going to create an encounter");
		}
		
		Encounter formEncounter = fes.getContext().getMode() == FormEntryContext.Mode.ENTER
		        ? fes.getSubmissionActions().getEncountersToCreate().get(0)
		        : encounter;
		
		// create a visit if necessary (note that this currently only works in real-time mode)
		if (createVisit != null && (createVisit) && visit == null) {
			visit = adtService.ensureActiveVisit(patient, sessionContext.getSessionLocation());
			fes.getContext().setVisit(visit);
		}
		
		// attach to the visit if it exists and not already attached
		if (visit != null && (encounter == null || encounter.getVisit() == null)) {
			try {
				new EncounterDomainWrapper(formEncounter).attachToVisit(visit);
			}
			catch (EncounterDateBeforeVisitStartDateException e) {
				validationErrors.add(
				    new FormSubmissionError("general-form-error", ui.message("htmlformentryui.datetimeAfterVisitDate")));
			}
			catch (EncounterDateAfterVisitStopDateException e) {
				validationErrors.add(
				    new FormSubmissionError("general-form-error", ui.message("htmlformentryui.datetimeBeforeVisitDate")));
			}
			
			if (validationErrors.size() > 0) {
				return returnHelper(validationErrors, fes, null);
			}
		}
		
		// Do actual encounter creation/updating
		try {
			fes.applyActions();
		}
		catch (Exception e) {
			validationErrors.addAll(convertToFormSubmissionErrors(e, ui));
			return returnHelper(validationErrors, fes, null);
		}
		
		if (returnUrl == null || !returnUrl.startsWith("post-message:")) { // hack to not render a toast during O3 integraton workflow
			request.getSession().setAttribute(UiCommonsConstants.SESSION_ATTRIBUTE_INFO_MESSAGE,
			    ui.message(
			        editMode ? "htmlformentryui.editHtmlForm.successMessage"
			                : "htmlformentryui.enterHtmlForm.successMessage",
			        ui.format(hf.getForm()), ui.encodeJavaScript(ui.format(patient))));
			request.getSession().setAttribute(UiCommonsConstants.SESSION_ATTRIBUTE_TOAST_MESSAGE, "true");
		}
		
		return returnHelper(null, fes, formEncounter);
	}
	
	private SimpleObject returnHelper(List<FormSubmissionError> validationErrors, FormEntrySession session,
	        Encounter encounter) {
		if (validationErrors == null || validationErrors.size() == 0) {
			String afterSaveUrl = session.getAfterSaveUrlTemplate();
			if (afterSaveUrl != null) {
				afterSaveUrl = afterSaveUrl.replaceAll("\\{\\{patient.id\\}\\}", session.getPatient().getId().toString());
				afterSaveUrl = afterSaveUrl.replaceAll("\\{\\{encounter.id\\}\\}",
				    session.getEncounter().getId().toString());
			}
			return SimpleObject.create("success", true, "encounterId", encounter.getId(), "encounterUuid",
			    encounter.getUuid(), "encounterTypeUuid",
			    encounter.getEncounterType() != null ? encounter.getEncounterType().getUuid() : null, "goToUrl",
			    afterSaveUrl);
		} else {
			Map<String, String> errors = new HashMap<String, String>();
			for (FormSubmissionError err : validationErrors) {
				if (err.getSourceWidget() != null)
					errors.put(session.getContext().getErrorFieldId(err.getSourceWidget()), err.getError());
				else
					errors.put(err.getId(), err.getError());
			}
			return SimpleObject.create("success", false, "errors", errors);
		}
	}
	
	private boolean hasNoTimeComponent(Date date) {
		return new DateMidnight(date).toDate().equals(date);
	}
	
	private void keepTimeComponentOfEncounterIfDateComponentHasNotChanged(Date previousEncounterDate,
	        Encounter formEncounter) {
		
		if (previousEncounterDate != null
		        && new DateMidnight(previousEncounterDate).equals(new DateMidnight(formEncounter.getEncounterDatetime()))) {
			formEncounter.setEncounterDatetime(previousEncounterDate);
		}
		
	}
	
	private void setupModel(FragmentModel model, FormEntrySession fes, VisitDomainWrapper visitDomainWrapper,
	        Boolean createVisit, String visitStartDatetime, String visitStopDatetime, UiUtils ui) {
		
		String currentDate = ui.convertTimezones() ? ui.format(new Date()) : ui.dateToISOString(new Date());
		model.addAttribute("visitStartDatetime", visitStartDatetime);
		model.addAttribute("visitStopDatetime", visitStopDatetime);
		model.addAttribute("currentDate", currentDate);
		model.addAttribute("command", fes);
		model.addAttribute("visit", visitDomainWrapper);
		
		if (createVisit != null) {
			model.addAttribute("createVisit", createVisit.toString());
		} else {
			model.addAttribute("createVisit", "false");
		}
		
		Integer hfeMajorVersion = 1;
		Integer hfeMinorVersion = 0;
		try {
			String hfeVersion = ModuleFactory.getStartedModuleById("htmlformentry").getVersion();
			String[] versionSplit = hfeVersion.split("\\.");
			hfeMajorVersion = Integer.valueOf(versionSplit[0]);
			hfeMinorVersion = Integer.valueOf(versionSplit[1]);
		}
		catch (Exception e) {
			log.warn("Unable to retrieve htmlformentry major and minor versions", e);
		}
		model.addAttribute("hfeMajorVersion", hfeMajorVersion);
		model.addAttribute("hfeMinorVersion", hfeMinorVersion);
		
	}
	
	private VisitDomainWrapper getVisitDomainWrapper(Visit visit, Encounter encounter, AdtService adtService) {
		// if we don't have a visit, but the encounter has a visit, use that
		if (visit == null && encounter != null) {
			visit = encounter.getVisit();
		}
		
		if (visit == null) {
			return null;
		} else {
			return adtService.wrap(visit);
		}
	}
	
	protected List<FormSubmissionError> convertToFormSubmissionErrors(Exception e, UiUtils ui) {
		List<FormSubmissionError> ret = new ArrayList<>();
		if (e instanceof ValidationException) {
			ValidationException ve = (ValidationException) e;
			List<ObjectError> objectErrors = ve.getErrors().getAllErrors();
			if (objectErrors != null) {
				for (ObjectError error : objectErrors) {
					String message = null;
					if (error.getCodes() != null) {
						for (String code : error.getCodes()) {
							if (StringUtils.isNotBlank(code)) {
								String translatedCode = ui.message(code, error.getArguments());
								if (!code.equals(translatedCode)) {
									message = translatedCode;
									break;
								}
							}
						}
					}
					if (message == null && StringUtils.isNotBlank(error.getCode())) {
						String translatedCode = ui.message(error.getCode(), error.getArguments());
						if (!error.getCode().equals(translatedCode)) {
							message = translatedCode;
						}
					}
					if (message == null) {
						message = error.getDefaultMessage();
					}
					if (StringUtils.isNotBlank(message)) {
						ret.add(new FormSubmissionError("general-form-error", ui.message(message)));
					}
				}
			}
		}
		if (ret.isEmpty()) {
			ret.add(new FormSubmissionError("general-form-error", ui.message(e.getMessage())));
		}
		return ret;
	}
}
