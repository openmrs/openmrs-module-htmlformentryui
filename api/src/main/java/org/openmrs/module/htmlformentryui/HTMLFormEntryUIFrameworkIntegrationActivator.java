/**
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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.Module;
import org.openmrs.module.ModuleFactory;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentryui.tag.ObsFromFragmentTagHandler;
import org.openmrs.module.htmlformentryui.tag.UiIncludeTagHandler;
import org.openmrs.module.htmlformentryui.tag.UiMessageTagHandler;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class HTMLFormEntryUIFrameworkIntegrationActivator extends BaseModuleActivator {

    protected final Log log = LogFactory.getLog(getClass());

    @Override
    public void started() {
        try {
            HtmlFormEntryService htmlFormEntryService = Context.getService(HtmlFormEntryService.class);
            htmlFormEntryService.addHandler(HtmlFormEntryUiConstants.HTMLFORMENTRY_UI_MESSAGE_TAG_NAME, new UiMessageTagHandler());
            htmlFormEntryService.addHandler(HtmlFormEntryUiConstants.HTMLFORMENTRY_UI_INCLUDE_TAG_NAME, new UiIncludeTagHandler());
            htmlFormEntryService.addHandler(HtmlFormEntryUiConstants.HTMLFORMENTRY_UI_OBS_FROM_FRAGMENT_TAG_NAME, new ObsFromFragmentTagHandler());
            
        }
        catch (Exception e) {
            Module mod = ModuleFactory.getModuleById(HtmlFormEntryUiConstants.MODULE_ID);
            ModuleFactory.stopModule(mod);
            throw new RuntimeException("failed to setup the " + HtmlFormEntryUiConstants.MODULE_ID + " module", e);
        }

        log.info("HTML Form Entry UI Framework Integration module started");
    }

    @Override
    public void stopped() {
        try {
            HtmlFormEntryService htmlFormEntryService = Context.getService(HtmlFormEntryService.class);
            htmlFormEntryService.getHandlers().remove(HtmlFormEntryUiConstants.HTMLFORMENTRY_UI_MESSAGE_TAG_NAME);
            htmlFormEntryService.getHandlers().remove(HtmlFormEntryUiConstants.HTMLFORMENTRY_UI_INCLUDE_TAG_NAME);
            htmlFormEntryService.getHandlers().remove(HtmlFormEntryUiConstants.HTMLFORMENTRY_UI_OBS_FROM_FRAGMENT_TAG_NAME);
        }
        catch (Exception ex) {
            // pass
        }
        log.info("HTML Form Entry UI Framework Integration module stopped");
    }
}
