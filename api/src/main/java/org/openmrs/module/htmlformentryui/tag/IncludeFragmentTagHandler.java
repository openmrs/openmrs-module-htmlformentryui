package org.openmrs.module.htmlformentryui.tag;

import java.util.Map;

import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler;

public class IncludeFragmentTagHandler extends SubstitutionTagHandler {
	
	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controllerActions,
	        Map<String, String> parameters) throws BadFormDesignException {
		IncludeFragmentSubmissionElement element = new IncludeFragmentSubmissionElement(session, parameters,Context.getConceptService());
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());
	}
	
}
