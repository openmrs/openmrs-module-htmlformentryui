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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.Translator;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class UiMessageTagHandlerTest {

    Locale currentLocale;
    MessageSourceService messageSourceService;
    private String messageCodeWithNoArg = "message.code.noarg";
    private String messageCodeWithArg = "message.code.arg";
    private String englishMessageWithNoArg = "Translated Message";
    private String englishMessageWithArg = "Translated Message {0}";
    private String frenchMessageWithNoArg = "Message traduit";
    private String frenchMessageWithArg = "Message traduit {0}";

    FormEntrySession formEntrySession;
    FormEntryContext formEntryContext;
    Translator translator;

    UiMessageTagHandler tagHandler;

    @Before
    public void setUp() throws Exception {
        currentLocale = Locale.ENGLISH;

        PowerMockito.mockStatic(Context.class, new Answer<Locale>() {
            @Override
            public Locale answer(InvocationOnMock invocationOnMock) throws Throwable {
                return currentLocale;
            }
        });

        messageSourceService = mock(MessageSourceService.class);
        when(messageSourceService.getMessage(messageCodeWithNoArg, null, Locale.ENGLISH)).thenReturn(englishMessageWithNoArg);
        when(messageSourceService.getMessage(eq(messageCodeWithArg), any(Object[].class), eq(Locale.ENGLISH))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) {
                Object[] messageArgs = (Object[]) invocationOnMock.getArguments()[1];
                return englishMessageWithArg.replace("{0}", messageArgs[0].toString());
            }
        });
        when(messageSourceService.getMessage(messageCodeWithNoArg, null, Locale.FRENCH)).thenReturn(frenchMessageWithNoArg);
        when(messageSourceService.getMessage(eq(messageCodeWithArg), any(Object[].class), eq(Locale.FRENCH))).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) {
                Object[] messageArgs = (Object[]) invocationOnMock.getArguments()[1];
                return frenchMessageWithArg.replace("{0}", messageArgs[0].toString());
            }
        });

        tagHandler = new UiMessageTagHandler(messageSourceService);

        formEntrySession = mock(FormEntrySession.class);
        formEntryContext = mock(FormEntryContext.class);
        translator = mock(Translator.class);
        when(formEntrySession.getContext()).thenReturn(formEntryContext);
        when(formEntryContext.getTranslator()).thenReturn(translator);

        when(translator.translate(Locale.ENGLISH.toString(), messageCodeWithNoArg)).thenReturn(englishMessageWithNoArg);
        when(translator.translate(Locale.FRENCH.toString(), messageCodeWithNoArg)).thenReturn(frenchMessageWithNoArg);
    }

    @Test
    public void testGetSubstitutionWithNoArguments() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put("code", messageCodeWithNoArg);

        String substitution = tagHandler.getSubstitution(formEntrySession, null, args);

        assertThat(substitution, is(englishMessageWithNoArg));
        verify(messageSourceService, never()).getMessage(anyString(), any(Object[].class), eq(Locale.ENGLISH));
    }

    @Test
    public void testGetSubstitutionWithArguments() throws Exception {
        String argValue = "Arg Value";

        Map<String, String> args = new HashMap<String, String>();
        args.put("code", messageCodeWithArg);
        args.put("arg0", argValue);

        String substitution = tagHandler.getSubstitution(formEntrySession, null, args);

        String expected = englishMessageWithArg.replace("{0}", argValue);
        assertThat(substitution, is(expected));
    }

    @Test
    public void testGetSubstitutionInDifferentLocaleThanTagHandlerWasInstantiatedIn() throws Exception {
        currentLocale = Locale.FRENCH;

        Map<String, String> args = new HashMap<String, String>();
        args.put("code", messageCodeWithNoArg);

        String substitution = tagHandler.getSubstitution(formEntrySession, null, args);

        assertThat(substitution, is(frenchMessageWithNoArg));
        verify(messageSourceService, never()).getMessage(anyString(), any(Object[].class), eq(Locale.FRENCH));
    }

    @Test
    public void testGetSubstitutionWithArgumentsInDifferentLocaleThanTagHandlerWasInstantiedIn() throws Exception {

        currentLocale = Locale.FRENCH;

        String argValue = "Arg Value";

        Map<String, String> args = new HashMap<String, String>();
        args.put("code", messageCodeWithArg);
        args.put("arg0", argValue);

        String substitution = tagHandler.getSubstitution(formEntrySession, null, args);

        String expected = frenchMessageWithArg.replace("{0}", argValue);
        assertThat(substitution, is(expected));
    }


}
