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

/**
 *
 */
public class HtmlFormEntryUiConstants {
	
	public static final String MODULE_ID = "htmlformentryui";
	
	public static final String HTMLFORMENTRY_UI_MESSAGE_TAG_NAME = "uimessage";
	
	public static final String HTMLFORMENTRY_UI_INCLUDE_TAG_NAME = "uiInclude";
	
	public static final String HTMLFORMENTRY_UI_OBS_FROM_FRAGMENT_TAG_NAME = "obsFromFragment";
	
	/**
	 * Global property: how many seconds before the user's session expires the "session about to
	 * expire" countdown is shown on html forms. A value of 0 or less disables the countdown (the
	 * user is still sent to the home page once the session has expired).
	 */
	public static final String GP_SESSION_EXPIRY_WARNING_SECONDS = MODULE_ID + ".sessionExpiryWarningSeconds";
	
	public static final int DEFAULT_SESSION_EXPIRY_WARNING_SECONDS = 60;
	
}
