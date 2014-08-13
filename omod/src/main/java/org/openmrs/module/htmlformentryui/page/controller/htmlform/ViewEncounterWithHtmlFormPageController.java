package org.openmrs.module.htmlformentryui.page.controller.htmlform;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Encounter;
import org.openmrs.module.emrapi.patient.PatientDomainWrapper;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.InjectBeans;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

public class ViewEncounterWithHtmlFormPageController {

    public void get(@RequestParam("encounter") Encounter encounter,
                    @RequestParam(value = "returnUrl", required = false) String returnUrl,
                    @RequestParam(value = "returnLabel", required = false) String returnLabel,
                    @InjectBeans PatientDomainWrapper patient,
                    @SpringBean("htmlFormEntryService") HtmlFormEntryService htmlFormEntryService,
                    UiUtils ui,
                    PageModel model) {

        patient.setPatient(encounter.getPatient());

        if (StringUtils.isEmpty(returnUrl)) {
            returnUrl = ui.pageLink("coreapps", "patientdashboard/patientDashboard", SimpleObject.create("patientId", patient.getId()));
        }
        if (StringUtils.isEmpty(returnLabel)) {
            returnLabel = ui.format(patient.getFormattedName());
        }

        model.addAttribute("patient", patient);
        model.addAttribute("visit", encounter.getVisit());
        model.addAttribute("encounter", encounter);
        model.addAttribute("returnUrl", returnUrl);
        model.addAttribute("returnLabel", returnLabel);

        HtmlForm htmlForm = htmlFormEntryService.getHtmlFormByForm(encounter.getForm());
        if (htmlForm == null) {
            throw new IllegalArgumentException("encounter.form is not an HTML Form: " + encounter.getForm());
        }
        model.addAttribute("htmlForm", htmlForm);
    }

}
