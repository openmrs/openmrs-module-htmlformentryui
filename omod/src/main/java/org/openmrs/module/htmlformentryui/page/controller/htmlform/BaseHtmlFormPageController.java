package org.openmrs.module.htmlformentryui.page.controller.htmlform;

import org.apache.commons.lang.StringUtils;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;

/**
 * Current just used to define standard utility methods used by both BaseEnterHtmlFormPageController and EditHtmlFormWithStandardUIPageController
 */
public abstract class BaseHtmlFormPageController {


    protected String determineReturnUrl(String returnUrl, Patient patient, Visit visit, UiUtils ui) {

        if (StringUtils.isEmpty(returnUrl)) {
            if (patient != null) {

                SimpleObject returnParams;

                if (visit == null) {
                    returnParams = SimpleObject.create("patientId", patient.getId());
                }
                else {
                    returnParams = SimpleObject.create("patientId", patient.getId(), "visitId", visit.getId());
                }
                returnUrl = ui.pageLink("coreapps", "patientdashboard/patientDashboard", returnParams);
            }
            else {
                returnUrl = "/" + ui.contextPath() + "index.html";
            }
        }
        return returnUrl;
    }

}
