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

import org.openmrs.Encounter;
import org.openmrs.Form;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentConfiguration;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

/**
 *
 */
public class ViewEncounterWithHtmlFormFragmentController {

    public void controller(FragmentConfiguration config,
                           @SpringBean("htmlFormEntryService") HtmlFormEntryService htmlFormEntryService,
                           @FragmentParam("encounter") Encounter encounter,
                           @FragmentParam(value = "htmlFormId", required = false) HtmlForm hf,
                           HttpSession httpSession,
                           UiUtils ui,
                           UiSessionContext sessionContext,
                           FragmentModel model) throws Exception {

    	model.addAttribute("encounterDatetime", encounter.getEncounterDatetime());
    	model.addAttribute("formattedEncounterDatetime", ui.dateToString(encounter.getEncounterDatetime()));
        model.addAttribute("html", getFormHtml(htmlFormEntryService, encounter, hf, ui, sessionContext, httpSession));
    }

    public SimpleObject getAsHtml(@SpringBean("htmlFormEntryService") HtmlFormEntryService htmlFormEntryService,
                                  @RequestParam("encounterId") Encounter encounter,
                                  @RequestParam(value = "htmlFormId", required = false) HtmlForm hf,
                                  UiUtils ui,
                                  UiSessionContext sessionContext,
                                  HttpSession httpSession) throws Exception {
        SimpleObject simpleObject = new SimpleObject();
        simpleObject.put("encounterDatetime", encounter.getEncounterDatetime());
        simpleObject.put("formattedEncounterDatetime", ui.dateToString(encounter.getEncounterDatetime()));
        simpleObject.put("html", getFormHtml(htmlFormEntryService, encounter, hf, ui, sessionContext, httpSession));
        return simpleObject;
    }

    private String getFormHtml(HtmlFormEntryService htmlFormEntryService,
                              Encounter encounter,
                              HtmlForm hf,
                              UiUtils ui,
                              UiSessionContext sessionContext,
                              HttpSession httpSession) throws Exception {
        if (hf == null) {
            Form form = encounter.getForm();
            if (form == null) {
                throw new IllegalArgumentException("Cannot view a form-less encounter unless you specify which form to use");
            }
            hf = htmlFormEntryService.getHtmlFormByForm(form);
            if (hf == null) {
                throw new IllegalArgumentException("The form for the specified encounter (" + encounter.getForm() + ") does not have an HtmlForm associated with it");
            }
        }

        FormEntrySession fes = new FormEntrySession(encounter.getPatient(), encounter, FormEntryContext.Mode.VIEW, hf, httpSession);
        fes.setAttribute("uiSessionContext", sessionContext);
        fes.setAttribute("uiUtils", ui);
        return fes.getHtmlToDisplay();
    }

}
