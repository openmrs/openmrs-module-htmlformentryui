package org.openmrs.module.htmlformentryui.tag;

import java.util.Map;

import org.openmrs.htmlformentryui.ConditionElement;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler;

public class ConditionTagHandler extends SubstitutionTagHandler {

	@Override
	protected String getSubstitution(FormEntrySession session, FormSubmissionController controller, Map<String, String> attributes)
			throws BadFormDesignException {
		ConditionElement element = new ConditionElement();
		String required = attributes.get("required");
		if (required != null) {
			element.setRequired(required.equalsIgnoreCase("true"));
		}
		session.getSubmissionController().addAction(element);
		return element.generateHtml(session.getContext());	
	}
}
