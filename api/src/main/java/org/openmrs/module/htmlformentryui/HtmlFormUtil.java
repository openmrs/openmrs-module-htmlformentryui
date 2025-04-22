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

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormFromFileLoader;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.resource.ResourceFactory;

import java.io.IOException;

public class HtmlFormUtil {
	
	public static HtmlForm getHtmlFormFromUiResource(ResourceFactory resourceFactory, FormService formService,
	        HtmlFormEntryService htmlFormEntryService, String providerAndPath, Encounter encounter) throws IOException {
		int ind = providerAndPath.indexOf(':');
		String provider = providerAndPath.substring(0, ind);
		String path = providerAndPath.substring(ind + 1);
		return getHtmlFormFromUiResource(resourceFactory, formService, htmlFormEntryService, provider, path, encounter);
	}
	
	public static HtmlForm getHtmlFormFromUiResource(ResourceFactory resourceFactory, FormService formService,
	        HtmlFormEntryService htmlFormEntryService, String providerName, String resourcePath, Encounter encounter)
	        throws IOException {
		
		String xml = null;
		
		// first, see if there is a specific version of the form referenced by version number
		if (encounter != null && encounter.getForm() != null && encounter.getForm().getVersion() != null) {
			String resourcePathWithVersion = resourcePath.replaceAll("\\.xml$", "") + "_v" + encounter.getForm().getVersion()
			        + ".xml";
			xml = resourceFactory.getResourceAsString(providerName, resourcePathWithVersion);
			// should be of the format <htmlform formUuid="..." formVersion="..." formEncounterType="...">...</htmlform>
		}
		
		// if not, use the bare resource path (without version number appended) to fetch the form
		if (xml == null) {
			xml = resourceFactory.getResourceAsString(providerName, resourcePath);
			
		}
		
		if (xml == null) {
			throw new IllegalArgumentException("No resource found at " + providerName + ":" + resourcePath);
		}
		
		return getHtmlFormFromResourceXml(formService, htmlFormEntryService, xml);
	}
	
	// the new method above with "encounter" is preferred if an encounter is available, see: https://issues.openmrs.org/browse/HTML-768
	public static HtmlForm getHtmlFormFromUiResource(ResourceFactory resourceFactory, FormService formService,
	        HtmlFormEntryService htmlFormEntryService, String providerAndPath) throws IOException {
		return getHtmlFormFromUiResource(resourceFactory, formService, htmlFormEntryService, providerAndPath,
		    (Encounter) null);
	}
	
	// the new method above with "encounter" is preferred if an encounter is available, see: https://issues.openmrs.org/browse/HTML-768
	public static HtmlForm getHtmlFormFromUiResource(ResourceFactory resourceFactory, FormService formService,
	        HtmlFormEntryService htmlFormEntryService, String providerName, String resourcePath) throws IOException {
		return getHtmlFormFromUiResource(resourceFactory, formService, htmlFormEntryService, providerName, resourcePath,
		    null);
	}
	
	public static HtmlForm getHtmlFormFromResourceXml(FormService formService, HtmlFormEntryService htmlFormEntryService,
	        String xml) {
		HtmlFormFromFileLoader loader = Context.getRegisteredComponents(HtmlFormFromFileLoader.class).get(0);
		return loader.saveHtmlForm(xml);
	}
	
	public static String determineReturnUrl(String returnUrl, String returnProviderName, String returnPageName,
	        Patient patient, Visit visit, UiUtils ui) {
		
		SimpleObject returnParams = null;
		
		if (patient != null) {
			if (visit == null) {
				returnParams = SimpleObject.create("patientId", patient.getId());
			} else {
				returnParams = SimpleObject.create("patientId", patient.getId(), "visitId", visit.getId());
			}
		}
		
		// first see if a return provider and page have been specified
		if (org.apache.commons.lang.StringUtils.isNotBlank(returnProviderName)
		        && org.apache.commons.lang.StringUtils.isNotBlank(returnPageName)) {
			return ui.pageLink(returnProviderName, returnPageName, returnParams);
		}
		
		// if not, see if a returnUrl has been specified
		if (org.apache.commons.lang.StringUtils.isNotBlank(returnUrl)) {
			return returnUrl;
		}
		
		// otherwise return to patient dashboard if we have a patient, but index if not
		if (returnParams != null && returnParams.containsKey("patientId")) {
			return ui.pageLink("coreapps", "patientdashboard/patientDashboard", returnParams);
		} else {
			return "/" + ui.contextPath() + "index.html";
		}
		
	}
	
	public static String determineReturnLabel(String returnLabel, Patient patient, UiUtils ui) {
		
		if (org.apache.commons.lang.StringUtils.isNotBlank(returnLabel)) {
			return ui.message(returnLabel);
		} else {
			return ui.encodeJavaScript(ui.format(patient));
		}
		
	}
}
