package org.openmrs.module.htmlformentryui.tag;

import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler;
import org.openmrs.module.htmlformentryui.element.ObsFromFragmentElement;
import org.openmrs.ui.framework.BasicUiUtils;
import org.openmrs.ui.framework.UiUtils;

public class ObsFromFragmentTagHandler extends SubstitutionTagHandler {

	private UiUtils uiUtils;
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controller, Map<String, String> parameters)
			throws BadFormDesignException {
		uiUtils = (UiUtils) (session.getAttribute("uiUtils") != null ? session.getAttribute("uiUtils") : Context.getRegisteredComponent("uiUtils", BasicUiUtils.class));
		ObsFromFragmentElement element = new ObsFromFragmentElement(parameters, uiUtils, session);
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());	
	}

	
}