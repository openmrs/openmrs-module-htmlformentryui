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
import org.openmrs.api.ConditionService;
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
	private ConditionService conditionService;
	private boolean required;
	// widgets
	private Widget conditionName;
	private DateWidget onSetDate;
	private DateWidget endDate;
	private RadioButtonsWidget conditionStatusWidget;
	private ErrorWidget endDateErrorWidget;
	private ErrorWidget conditionNameErrorWidget;
	private ErrorWidget conditionStatusErrorWidget;
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		FormEntryContext context = session.getContext();
		if (context.getMode() != Mode.VIEW) {
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
			conditionService = Context.getConditionService();
			conditionService.saveCondition(condition);
		}
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {		
		List<FormSubmissionError> ret = new ArrayList<FormSubmissionError>();
		Date givenOnsetDate = onSetDate.getValue(context, submission);
		Date givenEndDate = endDate.getValue(context, submission);
		String condition = submission.getParameter(context.getFieldName(conditionName));
		ConditionClinicalStatus status = getStatus(context, submission);
		
		if (context.getMode() != Mode.VIEW) {
			if (StringUtils.isBlank(condition) && required) {
				ret.add(new FormSubmissionError(context
	                    .getFieldName(conditionName), "htmlformentryui.conditionui.condition.required"));
			}	
			if (givenOnsetDate != null && givenEndDate != null) {
				if (givenOnsetDate.after(givenEndDate)) {
					ret.add(new FormSubmissionError(context
	                        .getFieldName(endDate), "htmlformentryui.conditionui.endDate.before.onsetDate.error"));
				}
			} 
			if (status == null && (required || StringUtils.isNotBlank(condition))) {
				ret.add(new FormSubmissionError(context
	                    .getFieldName(conditionStatusWidget), "htmlformentryui.conditionui.status.required"));
			}
		}
		return ret;
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder ret = new StringBuilder();
		ret.append("<div id=\"htmlformentryui-condition\">");
		ret.append(htmlForConditionSearchWidget(context));
		ret.append(htmlForConditionStatusWidgets(context));		
		ret.append(htmlForConditionDateWidgets(context));
		ret.append("</div>");
		return ret.toString();
	}
		
	private String htmlForConditionSearchWidget(FormEntryContext context) {
		List<ConceptClass> requiredClasses = new ArrayList<ConceptClass>();
		Set<Concept> initialConcepts = new HashSet<Concept>();
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		ConceptClass classCache = Context.getConceptService().getConceptClassByUuid("8d4918b0-c2cc-11de-8d13-0010c6dffd0f");
		if (classCache != null) {
			requiredClasses.add(classCache);
		}
		classCache = Context.getConceptService().getConceptClassByUuid("8d492b2a-c2cc-11de-8d13-0010c6dffd0f");
		if (classCache != null) {
			requiredClasses.add(classCache);
		}
		classCache = Context.getConceptService().getConceptClassByUuid("8d491a9a-c2cc-11de-8d13-0010c6dffd0f");
		if (classCache != null) {
			requiredClasses.add(classCache);
		}
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
		ret.append(" jq('#" + conditionNameTextInputId + "').css('min-width', '50%');\n");

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
	
	private String htmlForConditionStatusWidgets(FormEntryContext context) {
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		Option active = new Option(mss.getMessage("coreapps.conditionui.active.label"), "active", false);
		Option inactive = new Option(mss.getMessage("coreapps.conditionui.inactive.label"), "inactive", false);
		Option historyOf = new Option(mss.getMessage("htmlformentryui.conditionui.historyOf.label"), "history-of", false);
		conditionStatusWidget = new RadioButtonsWidget();
		conditionStatusErrorWidget = new ErrorWidget();
		conditionStatusWidget.addOption(active);
		conditionStatusWidget.addOption(inactive);
		conditionStatusWidget.addOption(historyOf);
		String radioGroupName = context.registerWidget(conditionStatusWidget);
		context.registerErrorWidget(conditionStatusWidget, conditionStatusErrorWidget);

		StringBuilder sb = new StringBuilder();
		sb.append("<div id=\"condition-status\">");
		sb.append(conditionStatusErrorWidget.generateHtml(context));
		sb.append(conditionStatusWidget.generateHtml(context));
		sb.append("<script>");		
		sb.append("jq(\"input[name='" + radioGroupName + "']\").change(function(e){\n" +
				"    if($(this).val() == 'active') {\n" + 
				"		document.getElementById('endDatePicker').style.visibility=\"hidden\"; \n" +
				"    } else {\n" + 
				"		document.getElementById('endDatePicker').style.visibility=\"visible\";\n" +
				"    }\n" + 
				"\n" + 
				"});");
		sb.append("</script>");
		sb.append("<style>");
		sb.append("#condition-status input {\n" + 
				"    display: inline;\n" + 
				"    float: none;\n" + 
				"}\n" + 
				"#condition-status label {\n" + 
				"    display: inline;\n" + 
				"}");
		sb.append("#condition-status {\n" +
				 "   padding:10px 0px;\n" +
				 " }");
		sb.append("</style>");
		sb.append("</div>");
		return sb.toString();	
	}
	
	private String htmlForConditionDateWidgets(FormEntryContext context) {
		onSetDate = new DateWidget();
		if (mss == null) {
			mss = Context.getMessageSourceService();		
		}
		String onsetDateTextInputId = context.registerWidget(onSetDate) + "-display";
		endDate = new DateWidget();
		endDateErrorWidget = new ErrorWidget();
		String endDateTextInputId = context.registerWidget(endDate) + "-display";
		context.registerErrorWidget(endDate, endDateErrorWidget);
		
		StringBuilder ret = new StringBuilder();
		ret.append("<ul>");
		ret.append("<li>");
		ret.append(onSetDate.generateHtml(context));
		ret.append("</li> <li>");
		ret.append("<span id=\"endDatePicker\">");
		ret.append(endDate.generateHtml(context));
		ret.append("</span>");
		ret.append("</li>");
		if (context.getMode() != Mode.VIEW) {
			ret.append(endDateErrorWidget.generateHtml(context));
		}
		ret.append("</ul> <br/>");
		ret.append("<script> jq('#" + onsetDateTextInputId + "').attr('placeholder',");
		ret.append(" '" + mss.getMessage("coreapps.conditionui.onsetdate") + "');");
		ret.append("jq('#" + endDateTextInputId + "').attr('placeholder',");
		ret.append(" '" + mss.getMessage("coreapps.stopDate.label") + "');");
		ret.append("</script>");
		ret.append("<style>");
		ret.append("#htmlformentryui-condition li {\n" + 
				"	width:30%;\n" + 
				"   float: left;\n" + 
				"}");
		ret.append("#htmlformentryui-condition ul {\n" + 
				"	display:flow-root;\n" + 
				"}");
		ret.append("</style>");
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
			if (((String)status).equals("history-of")) {
				return ConditionClinicalStatus.HISTORY_OF;
			}
		}
		return null;
	}

	public void setConditionName(Widget conditionName) {
		this.conditionName = conditionName;
	}

	public void setOnSetDate(DateWidget onSetDate) {
		this.onSetDate = onSetDate;
	}

	public void setEndDate(DateWidget endDate) {
		this.endDate = endDate;
	}

	public void setConditionStatusWidget(RadioButtonsWidget conditionStatusWidget) {
		this.conditionStatusWidget = conditionStatusWidget;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

}
