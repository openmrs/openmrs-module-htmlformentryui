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

package org.openmrs.module.htmlformentryui.tag;

import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 *
 */
public class UiMessageTagHandler extends SubstitutionTagHandler {

    MessageSourceService messageSourceService;

    public UiMessageTagHandler() {
        messageSourceService = Context.getMessageSourceService();
    }

    public UiMessageTagHandler(MessageSourceService messageSourceService) {
        this.messageSourceService = messageSourceService;
    }

    @Override
    protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions, Map<String, String> parameters) throws BadFormDesignException {
        String codeParam = parameters.get("code");
        if (codeParam == null) {
            return "Missing \"code\" attribute";
        }

        List<String> argList = new ArrayList<String>();
        int index = 0;
        while (true) {
            String argValue = parameters.get("arg" + index);
            if (argValue == null) {
                break;
            }
            argList.add(argValue);
            ++index;
        }
        Object[] args = argList.isEmpty() ? null : argList.toArray();

        Locale locale = Context.getLocale();

        // defer to the HFE translator if args = 0 (otherwise go directly to the message source since the HFE translator doesn't support arguments)
        // the HFE translator first checks any translations defined by the <translations> tag, and otherwise defers to the message source service

       String message = null;

       if (args == null || args.length == 0) {
            message = session.getContext().getTranslator().translate(locale.toString(), codeParam);
       }

       if (StringUtils.isBlank(message)) {
            message = messageSourceService.getMessage(codeParam, args, locale);
       }

        return  message;
    }

}
