package org.openmrs.htmlformentryui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.SingleOptionWidget;
import org.openmrs.module.htmlformentry.widget.Widget;
import org.openmrs.ui.framework.UiUtils;

public class ConditionElement implements HtmlGeneratorElement, FormSubmissionControllerAction  {
		
	private Widget conditionName;
	private DateWidget onSetDate;
	private DateWidget endDate;
	private RadioButtonsWidget conditionSatus;
	private String conditionNameTextInputId;
	private ErrorWidget onsetDateErrorWidget;
	private ErrorWidget endDateErrorWidget;
	private ErrorWidget conditionNameErrorWidget;
	
	@Override
	public void handleSubmission(FormEntrySession context, HttpServletRequest submission) {
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		Date givenOnsetDate = onSetDate.getValue(context, submission);
		if (conditionName.getValue(context, submission) == null) {
			ret.add(new FormSubmissionError(context
                    .getFieldName(conditionName), "Condition name can't be null"));
		}
		if (givenOnsetDate != null) {
			if (givenOnsetDate.after(new Date())) {
				ret.add(new FormSubmissionError(context
                        .getFieldName(onSetDate), "Onset date can't be after Today"));
			}
		}
		if (endDate.getValue(context, submission) != null) {
			Date givenEndDate = endDate.getValue(context, submission);
			if (givenOnsetDate != null) {
				if (givenOnsetDate.before(givenEndDate)) {
					ret.add(new FormSubmissionError(context
	                        .getFieldName(endDate), "Onset date can't be before end date"));
				}
			} else {
				ret.add(new FormSubmissionError(context
                        .getFieldName(onSetDate), "Onset date can't be null"));
			}
			
		}
		return ret;
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		MessageSourceService mss = Context.getMessageSourceService();		
		StringBuilder ret = new StringBuilder();
		ret.append("<div id=" + "\" condition \"" + ">");
		ret.append("<h5>");
		ret.append(mss.getMessage("coreapps.conditionui.addNewCondition"));
		ret.append("</h5>");
		ret.append("<div id=" + "\"conceptSearchWidget\"" + ">");
		ret.append(createConditionNameWidget(context));
		ret.append("</div>");
		ret.append("<div id=" + "\"conditionStatusDatePickers\"" + ">");
		ret.append(createConditionDateWidgets(context));
		ret.append("</div>");
		ret.append("<div id=" + "\"conditionStatus\"" + ">");
		ret.append(createConditionStatusWidgets(context));
		ret.append("</div>");
		ret.append("</div>");
		
		return ret.toString();
	}
	
	private String createConditionNameWidget(FormEntryContext context) {
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
		conditionNameTextInputId = context.registerWidget(conditionName);
		conditionNameErrorWidget = new ErrorWidget();
		context.registerErrorWidget(conditionName, conditionNameErrorWidget);
		
		String conditionNameAddButtonId = conditionNameTextInputId + "_button";
		StringBuilder ret = new StringBuilder();
		ret.append("<script> jq('#" + conditionNameTextInputId + "').attr('placeholder', 'Condition');");
		// remove the add button
		ret.append("jq('#" + conditionNameAddButtonId + "').remove();");
		ret.append("</script>");
		ret.append(conditionNameErrorWidget.generateHtml(context));
		return conditionName.generateHtml(context) + ret.toString();
	}
	
	private String createConditionStatusWidgets(FormEntryContext context) {
		Option active = new Option("Active", "active", false);
		Option inactive = new Option("Inactive", "inactive", false);
		conditionSatus = new RadioButtonsWidget();
		
		conditionSatus.addOption(active);
		conditionSatus.addOption(inactive);
		conditionSatus.setInitialValue("active");
		conditionSatus.setAnswerSeparator("");
		String radioGroupName = context.registerWidget(conditionSatus);
		
		StringBuilder sb = new StringBuilder();
		sb.append("<script>");		
		sb.append("jq(\"input[name='" + radioGroupName + "']\").change(function(e){\n" +
				"    if($(this).val() == 'active') {\n" + 
				"		jq('#endDatePicker').hide(); \n" +
				"    } else {\n" + 
				"		 jq('#endDatePicker').show(); \n" +
				"    }\n" + 
				"\n" + 
				"});");
		sb.append("</script>");		
		return conditionSatus.generateHtml(context) + sb.toString();
		
	}
	
	private String createConditionDateWidgets(FormEntryContext context) {
		onSetDate = new DateWidget();
		onsetDateErrorWidget = new ErrorWidget();
		String onsetDateTextInputId = context.registerWidget(onSetDate) + "-display";
		context.registerErrorWidget(onSetDate, onsetDateErrorWidget);
		
		endDate = new DateWidget();
		endDateErrorWidget = new ErrorWidget();
		String endDateTextInputId = context.registerWidget(endDate) + "-display";
		context.registerErrorWidget(endDate, endDateErrorWidget);
		
		StringBuilder ret = new StringBuilder();
		ret.append(onSetDate.generateHtml(context));
		ret.append(onsetDateErrorWidget.generateHtml(context));
		ret.append("<br/>");
		ret.append("<span id=\"endDatePicker\" hidden>" + endDate.generateHtml(context) + "</span>");
		ret.append("<br/>");
		ret.append("<script> jq('#" + onsetDateTextInputId + "').attr('placeholder', 'Onset Date');");
		ret.append("jq('#" + endDateTextInputId + "').attr('placeholder', 'End Date');");
		ret.append("</script>");
		ret.append(endDateErrorWidget.generateHtml(context));
		return ret.toString();
	}

}
