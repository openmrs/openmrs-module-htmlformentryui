package org.openmrs.htmlformentryui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.DynamicAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.ui.framework.UiUtils;

public class ConditionElement implements HtmlGeneratorElement, FormSubmissionControllerAction  {
		
	private Widget conditionName;
	private DateWidget onSetDate;
	private DateWidget endDate;
	private SingleOptionWidget conditionSatus;
	
	@Override
	public void handleSubmission(FormEntrySession arg0, HttpServletRequest arg1) {
		// TODO handle the htmlentry form submission
		
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext arg0, HttpServletRequest arg1) {
		// TODO handle the htmlentry form validation
		return Collections.emptyList();
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		MessageSourceService mss = Context.getMessageSourceService();		
		StringBuilder ret = new StringBuilder();
		ret.append("<div id=" + "\" condition \"" + ">");
		ret.append("<h5>");
		ret.append(mss.getMessage("coreapps.conditionui.addNewCondition"));
		ret.append("</h5>");
		ret.append("<div id=" + "\" conceptSearchWidget \"" + ">");
		ret.append(createConditionNameWidget(context).generateHtml(context));
		ret.append("</div>");
		ret.append("<div id=" + "\" conditionStatusDatePickers \"" + ">");
		ret.append(createOnSetDateWidget(context).generateHtml(context));
		ret.append(createEndDateWidget(context).generateHtml(context));
		ret.append("</div>");
		ret.append("<div id=" + "\" conditionStatus \"" + ">");
		ret.append(createConditionStatusWidget(context));
		ret.append("</div>");
		ret.append("</div>");
		
		return ret.toString();
	}
	
	private Widget createConditionNameWidget(FormEntryContext context) {
		List<ConceptClass> requiredClasses = new ArrayList<ConceptClass>();
		Set<Concept> initialConcepts = new HashSet<Concept>();
		requiredClasses.add(Context.getConceptService().getConceptClassByUuid("8d4918b0-c2cc-11de-8d13-0010c6dffd0f"));
		requiredClasses.add(Context.getConceptService().getConceptClassByUuid("8d492954-c2cc-11de-8d13-0010c6dffd0f"));
		requiredClasses.add(Context.getConceptService().getConceptClassByUuid("8d492b2a-c2cc-11de-8d13-0010c6dffd0f"));
		requiredClasses.add(Context.getConceptService().getConceptClassByUuid("8d491a9a-c2cc-11de-8d13-0010c6dffd0f"));
		
		for (ConceptClass cc : requiredClasses) {
			initialConcepts.addAll(Context.getConceptService().getConceptsByClass(cc));
		}
		
		conditionName = new DynamicAutocompleteWidget(new ArrayList<Concept>(initialConcepts), requiredClasses);
		context.registerWidget(conditionName);
		return conditionName;
	}
	
	private String createConditionStatusWidget(FormEntryContext context) {
		Option active = new Option("Active", "active", false);
		Option inactive = new Option("Inactive", "inactive", false);
		conditionSatus = new RadioButtonsWidget();
		
		conditionSatus.addOption(active);
		conditionSatus.addOption(inactive);
		conditionSatus.setInitialValue("active");
		
		String conditionSatusWidgetId = context.registerWidget(conditionSatus);
		
		StringBuilder sb = new StringBuilder();
		sb.append("<script type=\"text/javascript\">");
		sb.append("document.load = function() {\n" + 
				"	document.getElementsByName(" + conditionSatusWidgetId + ")[0].setAttribute(\"onclick\", \"toggleShowEndDate(this)\");\n" + 
				"	document.getElementsByName(" + conditionSatusWidgetId + ")[1].setAttribute(\"onclick\", \"toggleShowEndDate(this)\");\n" + 
				"\n" + 
				"}\n" + 
				"");
		sb.append("function toggleShowEndDate(option) {\n" + 
				"	if (option.value === \"inactive\") {\n" + 
				"		alert(\"inactive selected\");\n" + 
				"	} else {\n" + 
				"		alert(\"active selected\");\n" + 
				"	}\n" + 
				"}");
		
		sb.append("</script>");
		
		return conditionSatus.generateHtml(context) + sb.toString();
		
	}
	
	private Widget createOnSetDateWidget(FormEntryContext context) {
		onSetDate = new DateWidget();
		context.registerWidget(onSetDate);
		return onSetDate;
	}
	
	private Widget createEndDateWidget(FormEntryContext context) {
		endDate = new DateWidget();
		endDate.setHidden(true);
		context.registerWidget(endDate);
		return endDate;
	}

}
