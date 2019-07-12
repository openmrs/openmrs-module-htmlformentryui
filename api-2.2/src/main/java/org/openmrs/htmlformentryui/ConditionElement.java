package org.openmrs.htmlformentryui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.openmrs.CodedOrFreeText;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.openmrs.module.htmlformentry.widget.ConceptSearchAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.ErrorWidget;
import org.openmrs.module.htmlformentry.widget.Option;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.openmrs.module.htmlformentry.widget.Widget;

public class ConditionElement implements HtmlGeneratorElement, FormSubmissionControllerAction  {
	
	private MessageSourceService mss;
	private boolean required;
	
	// widgets
	private Widget conditionName;
	private DateWidget onSetDate;
	private DateWidget endDate;
	private RadioButtonsWidget conditionStatusWidget;
	private ErrorWidget onsetDateErrorWidget;
	private ErrorWidget endDateErrorWidget;
	private ErrorWidget conditionNameErrorWidget;
	private ErrorWidget conditionStatusErrorWidget;
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		FormEntryContext context = session.getContext();
		Condition condition = new Condition();
		CodedOrFreeText conditionConcept = new CodedOrFreeText();
		try {
			int conceptId = Integer.parseInt((String) conditionName.getValue(session.getContext(), submission));
			conditionConcept.setCoded(new Concept(conceptId));
			
		} catch(NumberFormatException e) {
			String nonCodedConcept = submission.getParameter(context.getFieldName(conditionName));
			conditionConcept.setNonCoded(nonCodedConcept);
		}
		condition.setCondition(conditionConcept);
		condition.setClinicalStatus(getStatus(context, submission));
		condition.setOnsetDate(onSetDate.getValue(context, submission));
		if (ConditionClinicalStatus.INACTIVE == getStatus(context, submission)) {
			condition.setEndDate(endDate.getValue(context, submission));
		}
		condition.setPatient(session.getPatient());
		Context.getConditionService().saveCondition(condition);
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		
		// TODO Localize error messages
		
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		Date givenOnsetDate = onSetDate.getValue(context, submission);
		String condition = submission.getParameter(context.getFieldName(conditionName));
		
		if (context.getMode() != Mode.VIEW) {
			
			if (StringUtils.isBlank(condition)) {
				ret.add(new FormSubmissionError(context
	                    .getFieldName(conditionName), "Condition name required"));
			}
			
			if (givenOnsetDate != null) {
				if (givenOnsetDate.after(new Date())) {
					ret.add(new FormSubmissionError(context
	                        .getFieldName(onSetDate), "Onset date can't be after Today"));
				}
			} else {
				ret.add(new FormSubmissionError(context
	                    .getFieldName(onSetDate), "Onset date required"));
			}
			
			ConditionClinicalStatus status = getStatus(context, submission);
			if (status != null) {
				if (ConditionClinicalStatus.INACTIVE == status) {
					Date givenEndDate = endDate.getValue(context, submission);
					if (givenEndDate != null) {
						if (givenOnsetDate != null) {
							if (givenOnsetDate.after(givenEndDate)) {
								ret.add(new FormSubmissionError(context
				                        .getFieldName(endDate), "End date can't be before onset date"));
							}

						} 
					} else {
						ret.add(new FormSubmissionError(context
		                        .getFieldName(endDate), "End date required"));
					}
				}	
		    } else {
		    	ret.add(new FormSubmissionError(context
	                    .getFieldName(conditionStatusWidget), "A Condition status is required"));
		    }
		}
		return ret;
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		mss = Context.getMessageSourceService();		
		StringBuilder ret = new StringBuilder();
		ret.append("<h5>");
		ret.append(mss.getMessage("coreapps.conditionui.addNewCondition"));
		ret.append("</h5>");
		ret.append(createConditionNameWidget(context));
		ret.append(createConditionDateWidgets(context));
		ret.append(createConditionStatusWidgets(context));		
		return ret.toString();
	}
		
	private String createConditionNameWidget(FormEntryContext context) {
		List<ConceptClass> requiredClasses = new ArrayList<ConceptClass>();
		Set<Concept> initialConcepts = new HashSet<Concept>();
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		
		requiredClasses.add(Context.getConceptService().getConceptClassByUuid("8d4918b0-c2cc-11de-8d13-0010c6dffd0f"));
		requiredClasses.add(Context.getConceptService().getConceptClassByUuid("8d492954-c2cc-11de-8d13-0010c6dffd0f"));
		requiredClasses.add(Context.getConceptService().getConceptClassByUuid("8d492b2a-c2cc-11de-8d13-0010c6dffd0f"));
		requiredClasses.add(Context.getConceptService().getConceptClassByUuid("8d491a9a-c2cc-11de-8d13-0010c6dffd0f"));
		
		for (ConceptClass cc : requiredClasses) {
			initialConcepts.addAll(Context.getConceptService().getConceptsByClass(cc));
		}
		
		conditionName = new ConceptSearchAutocompleteWidget(new ArrayList<Concept>(initialConcepts), requiredClasses);
		String conditionNameTextInputId = context.registerWidget(conditionName);
		conditionNameErrorWidget = new ErrorWidget();
		context.registerErrorWidget(conditionName, conditionNameErrorWidget);
		
		StringBuilder ret = new StringBuilder();
		ret.append(conditionName.generateHtml(context));
		if (context.getMode() != Mode.VIEW) {
			ret.append(conditionNameErrorWidget.generateHtml(context));
		}
		ret.append("\n<script>jq('#" + conditionNameTextInputId + "').attr('placeholder',");
		ret.append(" '" + mss.getMessage("coreapps.conditionui.condition") + "');\n");
		// Add support for non-coded concept values.
		// This a hack to let the autocomplete widget accept values that aren't part of the concept list.
		ret.append("jq('#" + conditionNameTextInputId + "').blur(function(e){\n");
		ret.append("     var valueAttr = jq('#" + conditionNameTextInputId + "_hid" + "').attr('value');\n");
		ret.append("     if(valueAttr === \"ERROR\"){\n");
		ret.append("        jq('#" + conditionNameTextInputId + "_hid" + "').attr('value', '');\n");
		ret.append("     }\n");
		ret.append("});\n");				
		ret.append("</script>\n");
		return ret.toString();

	}
	
	private String createConditionStatusWidgets(FormEntryContext context) {
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		Option active = new Option(mss.getMessage("coreapps.conditionui.active.label"), "active", false);
		Option inactive = new Option(mss.getMessage("coreapps.conditionui.inactive.label"), "inactive", false);
		conditionStatusWidget = new RadioButtonsWidget();
		conditionStatusErrorWidget = new ErrorWidget();
		
		conditionStatusWidget.addOption(active);
		conditionStatusWidget.addOption(inactive);
		conditionStatusWidget.setInitialValue("active");
		conditionStatusWidget.setAnswerSeparator("");
		String radioGroupName = context.registerWidget(conditionStatusWidget);
		context.registerErrorWidget(conditionStatusWidget, conditionStatusErrorWidget);

		StringBuilder sb = new StringBuilder();
		sb.append(conditionStatusErrorWidget.generateHtml(context));
		
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
		return conditionStatusWidget.generateHtml(context) + sb.toString();
		
	}
	
	private String createConditionDateWidgets(FormEntryContext context) {
		onSetDate = new DateWidget();
		onsetDateErrorWidget = new ErrorWidget();
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		
		String onsetDateTextInputId = context.registerWidget(onSetDate) + "-display";
		context.registerErrorWidget(onSetDate, onsetDateErrorWidget);
		
		endDate = new DateWidget();
		endDateErrorWidget = new ErrorWidget();
		String endDateTextInputId = context.registerWidget(endDate) + "-display";
		context.registerErrorWidget(endDate, endDateErrorWidget);
		
		StringBuilder ret = new StringBuilder();
		ret.append(onSetDate.generateHtml(context));
		
		if (context.getMode() != Mode.VIEW) {
			ret.append(onsetDateErrorWidget.generateHtml(context));
		}
		
		ret.append("<br/>");
		ret.append("<span id=\"endDatePicker\" hidden>");
		ret.append(endDate.generateHtml(context));
		ret.append("</span> <br/>");
		if (context.getMode() != Mode.VIEW) {
			ret.append(endDateErrorWidget.generateHtml(context));
		}
		ret.append("<script> jq('#" + onsetDateTextInputId + "').attr('placeholder',");
		ret.append(" '" + mss.getMessage("coreapps.conditionui.onsetdate") + "');");
		ret.append("jq('#" + endDateTextInputId + "').attr('placeholder',");
		ret.append(" '" + mss.getMessage("coreapps.stopDate.label") + "');");
		ret.append("</script>");
		return ret.toString();
	}

	private ConditionClinicalStatus getStatus(FormEntryContext context, HttpServletRequest request) {
		if (conditionStatusWidget == null) {
			return null;
		}
		Object status = conditionStatusWidget.getValue(context, request);
		if (status != null) {			
			if (((String)status).equals("active")) {
				return ConditionClinicalStatus.ACTIVE;
			}
			if (((String)status).equals("inactive")) {
				return ConditionClinicalStatus.INACTIVE;
			}
		}
		return null;
	}
}
