package org.openmrs.module.htmlformentryui.page.controller.htmlform;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.FormService;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentryui.HtmlFormUtil;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.framework.resource.ResourceFactory;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

public class BaseEditHtmlFormPageController extends BaseHtmlFormPageController {

    public void get(@RequestParam("encounterId") Encounter encounter,
                    @RequestParam("patientId") Patient patient, // explicitly require this instead of inferring from encounter because this sets up the global context
                    @RequestParam(value = "returnUrl", required = false) String returnUrl,
                    @RequestParam(value = "returnProvider", required =  false) String returnProvider,
                    @RequestParam(value = "returnPage", required = false) String returnPage,
                    @RequestParam(value = "returnLabel", required = false) String returnLabel,
                    @RequestParam(value = "breadcrumbOverride", required = false) String breadcrumbOverride,
                    @SpringBean("htmlFormEntryService") HtmlFormEntryService htmlFormEntryService,
                    @SpringBean("formService") FormService formService,
                    @RequestParam(value = "definitionUiResource", required = false) String definitionUiResource,
                    @SpringBean("coreResourceFactory") ResourceFactory resourceFactory,
                    UiUtils ui,
                    PageModel pageModel) throws Exception{

        // TODO: maybe this should be merged in with BaseEnterHtmlFormPageController?

        if (!encounter.getPatient().equals(patient)) {
            throw new IllegalArgumentException("encounter.patient != patient");
        }


        HtmlForm htmlForm = null;

        if (org.apache.commons.lang.StringUtils.isNotBlank(definitionUiResource)) {
            htmlForm = HtmlFormUtil.getHtmlFormFromUiResource(resourceFactory, formService, htmlFormEntryService, definitionUiResource);
            if (htmlForm == null) {
                throw new IllegalArgumentException("No form found for resource " + definitionUiResource);
            }
        } else {
            if (encounter.getForm() == null) {
                throw new IllegalArgumentException("encounter.form is null");
            }
            htmlForm = htmlFormEntryService.getHtmlFormByForm(encounter.getForm());
        }
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
        pageModel.addAttribute("definitionUiResource", definitionUiResource);
        pageModel.addAttribute("breadcrumbOverride", breadcrumbOverride);
    }

}
