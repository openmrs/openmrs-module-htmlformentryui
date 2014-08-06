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

package org.openmrs.module.htmlformentryui.page.controller.htmlform;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 */
public class EditHtmlFormWithStandardUiPageController extends BaseHtmlFormPageController {

    public void get(@RequestParam("encounterId") Encounter encounter,
                    @RequestParam("patientId") Patient patient, // explicitly require this instead of inferring from encounter because this sets up the global context
                    @RequestParam(value = "returnUrl", required = false) String returnUrl,
                    @RequestParam(value = "returnProvider", required =  false) String returnProvider,
                    @RequestParam(value = "returnPage", required = false) String returnPage,
                    @RequestParam(value = "returnLabel", required = false) String returnLabel,
                    @RequestParam(value = "breadcrumbOverride", required = false) String breadcrumbOverride,
                    @SpringBean("htmlFormEntryService") HtmlFormEntryService htmlFormEntryService,
                    UiUtils ui,
                    PageModel pageModel) {

        // TODO: maybe this should be merged in with BaseEnterHtmlFormPageController?

        if (!encounter.getPatient().equals(patient)) {
            throw new IllegalArgumentException("encounter.patient != patient");
        }

        if (encounter.getForm() == null) {
            throw new IllegalArgumentException("encounter.form is null");
        }
        HtmlForm htmlForm = htmlFormEntryService.getHtmlFormByForm(encounter.getForm());
        if (htmlForm == null) {
            throw new IllegalArgumentException("encounter.form is not an HTML Form: " + encounter.getForm());
        }

        returnUrl = determineReturnUrl(returnUrl, returnProvider, returnPage, patient, encounter.getVisit(), ui);
        returnLabel = determineReturnLabel(returnLabel, patient, ui);

        pageModel.addAttribute("encounter", encounter);
        pageModel.addAttribute("patient", patient);
        pageModel.addAttribute("htmlForm", htmlForm);
        pageModel.addAttribute("returnUrl", returnUrl);
        pageModel.addAttribute("returnLabel", returnLabel);
        pageModel.addAttribute("breadcrumbOverride", breadcrumbOverride);
    }

}
