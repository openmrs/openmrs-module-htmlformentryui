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
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.Obs;
import org.openmrs.api.FormService;
import org.openmrs.module.appframework.feature.FeatureToggleProperties;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.disposition.DispositionService;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentryui.HtmlFormUtil;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.openmrs.ui.framework.resource.ResourceFactory;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 *
 */
public class ViewEncounterWithHtmlFormFragmentController extends BaseHtmlFormFragmentController {

    public void controller(@SpringBean("htmlFormEntryService") HtmlFormEntryService htmlFormEntryService,
                           @SpringBean("formService") FormService formService,
                           @SpringBean("coreResourceFactory") ResourceFactory resourceFactory,
                           @SpringBean EmrApiProperties emrApiProperties,
                           @SpringBean DispositionService dispositionService,
                           @SpringBean FeatureToggleProperties featureToggleProperties,
                           @FragmentParam("encounter") Encounter encounter,
                           @FragmentParam(value = "htmlFormId", required = false) HtmlForm hf,
                           @FragmentParam(value = "definitionUiResource", required = false) String definitionUiResource,
                           HttpSession httpSession,
                           UiUtils ui,
                           UiSessionContext sessionContext,
                           FragmentModel model) throws Exception {

        VisitDomainWrapper visitWrapper = encounter.getVisit() == null ? null : new VisitDomainWrapper(encounter.getVisit(), emrApiProperties, dispositionService);

        model.addAttribute("encounterDatetime", encounter.getEncounterDatetime());
        model.addAttribute("formattedEncounterDatetime", ui.formatDatetimePretty(encounter.getEncounterDatetime()));

        FormEntrySession fes = getFormEntrySession(htmlFormEntryService, formService, resourceFactory, encounter, hf,
                definitionUiResource, ui, sessionContext, httpSession, visitWrapper, featureToggleProperties);

        generateHtmlAndAddToModel(model, fes);
    }

    public SimpleObject getAsHtml(@SpringBean("htmlFormEntryService") HtmlFormEntryService htmlFormEntryService,
                                  @SpringBean("formService") FormService formService,
                                  @SpringBean("coreResourceFactory") ResourceFactory resourceFactory,
                                  @SpringBean EmrApiProperties emrApiProperties,
                                  @SpringBean DispositionService dispositionService,
                                  @SpringBean FeatureToggleProperties featureToggleProperties,
                                  @RequestParam("encounterId") Encounter encounter,
                                  @RequestParam(value = "htmlFormId", required = false) HtmlForm hf,
                                  @RequestParam(value = "definitionUiResource", required = false) String definitionUiResource,
                                  UiUtils ui,
                                  UiSessionContext sessionContext,
                                  HttpSession httpSession) throws Exception {

        VisitDomainWrapper visitWrapper = encounter.getVisit() == null ? null : new VisitDomainWrapper(encounter.getVisit(), emrApiProperties, dispositionService);

        SimpleObject simpleObject = new SimpleObject();
        simpleObject.put("encounterDatetime", encounter.getEncounterDatetime());
        simpleObject.put("formattedEncounterDatetime", ui.formatDatetimePretty(encounter.getEncounterDatetime()));

        FormEntrySession fes = getFormEntrySession(htmlFormEntryService, formService, resourceFactory, encounter, hf,
                definitionUiResource, ui, sessionContext, httpSession, visitWrapper, featureToggleProperties);

        generateHtmlAndAddToModel(simpleObject, fes);
        return simpleObject;
    }

    private FormEntrySession getFormEntrySession(HtmlFormEntryService htmlFormEntryService,
                               FormService formService,
                               ResourceFactory resourceFactory,
                               Encounter encounter,
                               HtmlForm hf,
                               String definitionUiResource,
                               UiUtils ui,
                               UiSessionContext sessionContext,
                               HttpSession httpSession,
                               VisitDomainWrapper visitDomainWrapper, FeatureToggleProperties featureToggleProperties) throws Exception {
        if (hf == null) {
            if (StringUtils.isNotBlank(definitionUiResource)) {
                hf = HtmlFormUtil.getHtmlFormFromUiResource(resourceFactory, formService, htmlFormEntryService, definitionUiResource);
                if (hf == null) {
                    throw new IllegalArgumentException("No form found for resource " + definitionUiResource);
                }
            }
            else {
                Form form = encounter.getForm();
                if (form == null) {
                    throw new IllegalArgumentException("Cannot view a form-less encounter unless you specify which form to use");
                }
                hf = htmlFormEntryService.getHtmlFormByForm(form);
                if (hf == null) {
                    throw new IllegalArgumentException("The form for the specified encounter (" + encounter.getForm() + ") does not have an HtmlForm associated with it");
                }
            }
        }

        FormEntrySession fes = new FormEntrySession(encounter.getPatient(), encounter, FormEntryContext.Mode.VIEW, hf, httpSession);
        fes.setAttribute("uiSessionContext", sessionContext);
        fes.setAttribute("uiUtils", ui);
        setupVelocityContext(fes, visitDomainWrapper, ui, sessionContext, featureToggleProperties);
        setupFormEntrySession(fes, visitDomainWrapper, null, ui, sessionContext, null);
        return fes;

    }



    private void generateHtmlAndAddToModel(Map<String,Object> map, FormEntrySession fes) throws Exception {

        // "hasExistingObs"is a bit of a hack: we count the number of existing obs before and after rendering the html--when creating the form entry session,
        // the HFE module populates the "existingObs" list with all obs from the associated encounter; then, when rendering the form, as the HFE module
        // matches the existing obs to obs tags on the form, it removes the obs from the list; therefore, we assume that if
        // numberOfExistingObs - numberOfExistingObsAfterFormGeneration > 0, it means the form has a match; (we don't just base this on the initial
        // size of the existingObs on the encounter for the use case where a form represents only *part* of an encounter, and so although the
        // encounter may have obs, the form does not)
        // example use case: in PIH Core we use "hasExistingObs" to determine the background color of section, which indicates whether a section has been completed or not

        int numberOfExistingObs = getNumberOfExistingObs(fes);
        int numberOfExistingObsInGroups = getNumberOfExistingObsInGroups(fes);

        String htmlToDisplay = fes.getHtmlToDisplay();
        map.put("html", htmlToDisplay);

        int numberOfExistingObsAfterFormGeneration = getNumberOfExistingObs(fes);
        int numberOfExistingObsInGroupsAfterFormGeneration = getNumberOfExistingObsInGroups(fes);

        map.put("hasExistingObs", numberOfExistingObs - numberOfExistingObsAfterFormGeneration > 0
                || numberOfExistingObsInGroups - numberOfExistingObsInGroupsAfterFormGeneration > 0);

    }

    private int getNumberOfExistingObs(FormEntrySession fes) {
        if (fes.getContext().getExistingObs() == null) {
            return 0;
        }
        else {
            int count = 0;
            for (Concept concept : fes.getContext().getExistingObs().keySet()) {
                count = count + fes.getContext().getExistingObs().get(concept).size();
            }
            return count;
        }
    }

    // currently does not handle nested obs groups
    private int getNumberOfExistingObsInGroups(FormEntrySession fes) {
        if (fes.getContext().getExistingObsInGroups() == null) {
            return 0;
        }
        else {
            int count = 0;
            for (Obs obs : fes.getContext().getExistingObsInGroups().keySet()) {
                count = count + fes.getContext().getExistingObsInGroups().get(obs).size();
            }
            return count;
        }
    }
}
