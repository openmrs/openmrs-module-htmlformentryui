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

package org.openmrs.module.htmlformentryui.converter;

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Treats the string as a pk id or uuid of an HtmlForm
 */
@Component
public class StringToHtmlFormConverter implements Converter<String, HtmlForm> {

    private Pattern onlyDigits = Pattern.compile("\\d+");

    @Autowired
    @Qualifier("htmlFormEntryService")
    HtmlFormEntryService htmlFormEntryService;

    public HtmlForm convert(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        else if (onlyDigits.matcher(source).matches()) {
            return htmlFormEntryService.getHtmlForm(Integer.valueOf(source));
        }
        else {
            return htmlFormEntryService.getHtmlFormByUuid(source);
        }
    }
}
