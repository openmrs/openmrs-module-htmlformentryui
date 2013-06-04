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

package org.openmrs.module.htmlformentryui;

import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.ui.framework.resource.ResourceFactory;
import org.openmrs.util.OpenmrsUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;

/**
 *
 */
public class HtmlFormUtil {

    public static HtmlForm getHtmlFormFromUiResource(ResourceFactory resourceFactory, FormService formService, HtmlFormEntryService htmlFormEntryService, String providerAndPath) throws IOException {
        int ind = providerAndPath.indexOf(':');
        String provider = providerAndPath.substring(0, ind);
        String path = providerAndPath.substring(ind + 1);
        return getHtmlFormFromUiResource(resourceFactory, formService, htmlFormEntryService, provider, path);
    }

    public static HtmlForm getHtmlFormFromUiResource(ResourceFactory resourceFactory, FormService formService, HtmlFormEntryService htmlFormEntryService, String providerName, String resourcePath) throws IOException {
        String xml = resourceFactory.getResourceAsString(providerName, resourcePath);
        // should be of the format <htmlform formUuid="..." formVersion="..." formEncounterType="...">...</htmlform>

        if (xml == null) {
            throw new IllegalArgumentException("No resource found at " + providerName + ":" + resourcePath);
        }

        Form form = null;
        try {
            Document doc = HtmlFormEntryUtil.stringToDocument(xml);
            Node htmlFormNode = HtmlFormEntryUtil.findChild(doc, "htmlform");
            String formUuid = getAttributeValue(htmlFormNode, "formUuid");
            if (formUuid == null) {
                throw new IllegalArgumentException("formUuid is required");
            }
            form = formService.getFormByUuid(formUuid);
            boolean needToSaveForm = false;
            if (form == null) {
                form = new Form();
                form.setUuid(formUuid);
                needToSaveForm = true;
            }

            String formName= getAttributeValue(htmlFormNode, "formName");
            if (!OpenmrsUtil.nullSafeEquals(form.getName(), formName)) {
                form.setName(formName);
                needToSaveForm = true;
            }

            String formDescription = getAttributeValue(htmlFormNode, "formDescription");
            if (!OpenmrsUtil.nullSafeEquals(form.getDescription(), formDescription)) {
                form.setDescription(formDescription);
                needToSaveForm = true;
            }

            String formVersion = getAttributeValue(htmlFormNode, "formVersion");
            if (!OpenmrsUtil.nullSafeEquals(form.getVersion(), formVersion)) {
                form.setVersion(formVersion);
                needToSaveForm = true;
            }

            String formEncounterType = getAttributeValue(htmlFormNode, "formEncounterType");
            EncounterType encounterType = formEncounterType == null ? null : HtmlFormEntryUtil.getEncounterType(formEncounterType);
            if (encounterType != null && !OpenmrsUtil.nullSafeEquals(form.getEncounterType(), encounterType)) {
                form.setEncounterType(encounterType);
                needToSaveForm = true;
            }

            if (needToSaveForm) {
                formService.saveForm(form);
            }

            HtmlForm htmlForm = htmlFormEntryService.getHtmlFormByForm(form);
            boolean needToSaveHtmlForm = false;
            if (htmlForm == null) {
                htmlForm = new HtmlForm();
                htmlForm.setForm(form);
                needToSaveHtmlForm = true;
            }
            if (!OpenmrsUtil.nullSafeEquals(htmlForm.getXmlData(), xml)) {
                htmlForm.setXmlData(xml);
                needToSaveHtmlForm = true;
            }
            if (needToSaveHtmlForm) {
                htmlFormEntryService.saveHtmlForm(htmlForm);
            }
            return htmlForm;

        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse XML and build Form and HtmlForm", e);
        }
    }

    private static String getAttributeValue(Node htmlForm, String attributeName) {
        Node item = htmlForm.getAttributes().getNamedItem(attributeName);
        return item == null ? null : item.getNodeValue();
    }

}
