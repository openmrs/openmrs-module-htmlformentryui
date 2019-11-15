package org.openmrs.module.htmlformentryui.element;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDatatype;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.openmrs.module.htmlformentryui.tag.UiIncludeTagHandler;
import org.openmrs.ui.framework.FragmentException;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.WebConstants;
import org.openmrs.ui.framework.page.PageAction;

public class ObsFromFragmentElement implements HtmlGeneratorElement, FormSubmissionControllerAction {

	private UiUtils uiUtils;
	private Map<String, Object> fragmentParams;
	private Concept concept;
	private String viewProvider;
	private String fragment;
	private String initFragmentParamName;
		
	// For tests only
	public ObsFromFragmentElement() {
		
	}
	
	public ObsFromFragmentElement(Map<String, String> parameters, UiUtils uiUtils, FormEntrySession session) {
		this.uiUtils = uiUtils;
		concept = HtmlFormEntryUtil.getConcept(parameters.get("conceptId"));
		if (concept == null) {
			throw new IllegalArgumentException("Cannot find concept with id: " + parameters.get("conceptId"));
		}
		viewProvider = parameters.get("provider");
		if (StringUtils.isBlank(viewProvider)) {
			throw new IllegalArgumentException("Parameter provider cannot be blank");
		}
		fragment = parameters.get("fragment");
		if (StringUtils.isBlank(fragment)) {
			throw new IllegalArgumentException("Parameter fragment cannot be blank");
		}
		initFragmentParamName = parameters.get("initFragmentParamName");
		if (StringUtils.isBlank(initFragmentParamName)) {
			throw new IllegalArgumentException("Parameter initFragmentParamName cannot be blank");
		}
		StringBuilder fragmentUrlBuilder = new StringBuilder();
		fragmentUrlBuilder.append(fragment);
		fragmentUrlBuilder.append("?");
        // evaluate any velocity expressions
		String fragmentParamsString = session.evaluateVelocityExpression(parameters.get("fragmentParams"));
		// replace any whitespace with a '+'
		String paramsWithoutWhiteSpace = fragmentParamsString.replaceAll("\\s+", "+");
		fragmentUrlBuilder.append(paramsWithoutWhiteSpace);
		try {
		  fragmentParams = UiIncludeTagHandler.paramsToMap(fragmentUrlBuilder.toString());
													
		} catch (URISyntaxException e) {
			throw new FragmentException("Invalid fragment URI: " + fragmentUrlBuilder.toString() , e);
		}
	}
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest request) {
		if (session.getContext().getMode().equals(FormEntryContext.Mode.VIEW)) {
			return;
		}
		String formFieldName = getFormFieldName();
		if (formFieldName != null) {
			String valueText = request.getParameter(formFieldName);
			if (StringUtils.isBlank(valueText)) {
				return;
			}
			if (session.getContext().getMode().equals(FormEntryContext.Mode.EDIT)) {
				List<Obs> existingObservations = session.getContext().removeExistingObs(concept);
				if (CollectionUtils.isNotEmpty(existingObservations)) {
					for (Obs obs : existingObservations) {
						if (obs.getComment().equals(formFieldName)) {
							session.getSubmissionActions().modifyObs(existingObservations.get(0), concept, convertToType(valueText), 
									new Date(), null, formFieldName);
							return;
						}
					}
				}
			}
			session.getSubmissionActions().createObs(concept, convertToType(valueText), new Date(), null,
					// Set the formFieldName of the widget as the Obs comment 
					// this helps to map an Obs to it's fragment widget for cases were the same concept
					// was used in more than one <obsFromFragment/> tag on the same form 
					formFieldName);
		}
	}

	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest request) {
		return Collections.emptyList();
	}

	@Override
	public String generateHtml(FormEntryContext context) {
		String wrapperDivId = getFormFieldName() + "-wrapper";
		initializeFragment(context);
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("<div id=\"" + wrapperDivId + "\">");
			sb.append(uiUtils.includeFragment(viewProvider, fragment, fragmentParams));
			sb.append("</div>");
			if (FormEntryContext.Mode.VIEW == context.getMode()) {
				// disable input widgets while in view mode
				sb.append("<script>");
				sb.append("jq(\"#" + wrapperDivId + " :input\").attr(\"disabled\", \"disabled\");\n");
				if (fragment.endsWith("datetimepicker")) {
					// if datetimepicker fragment, disable the calendar icon
					sb.append("jq('#" + wrapperDivId + " .add-on').eq(0).off('click');");
				}
				sb.append("</script>");	
			}
			return sb.toString();
		} catch (NullPointerException e) {
			// if we are validating/submitting the form, then this method is being called from a fragment action method
            // and the UiUtils we have access to doesn't have a FragmentIncluder. That's okay, because we don't actually
            // need to generate the HTML, so we can pass on this exception.
            // (This is hacky, but I don't see a better way to do it.)
            return "Submitting the form, so we don't generate HTML";
		} catch (PageAction e) {
			throw new IllegalStateException("Tried to include a fragment that threw a PageAction.", e);
		}
	}
	
	protected Object convertToType(String val) {
		ConceptDatatype dataType = concept.getDatatype();
		if (dataType.isDate()) {
			return HtmlFormEntryUtil.convertToType(val, Date.class);
		}
		if (dataType.isDateTime() || dataType.isTime()) {
			try {
				DateFormat df = new SimpleDateFormat(WebConstants.DATE_FORMAT_DATETIME);
				df.setLenient(false);
				return df.parse(val);
			} catch (ParseException e) {
				throw new IllegalArgumentException("Failed to parse date: " + val, e);
			}
		}
		if (dataType.isCoded()) {
			for (ConceptAnswer answer : concept.getAnswers()) {
				if (answer.getAnswerConcept().getName().getName().equals(val)) {
					return answer.getAnswerConcept();
				}
			}
		}
		if (dataType.isBoolean()) {
			return Boolean.valueOf(val);
		}
		if (dataType.isNumeric()) {
			return Double.parseDouble(val.toString());
		}
		return val;
	}
	
	/**
	 * Gets fragment {@link Option}s from the associated question
	 * 
	 * @param question {@link Concept} from which answers will be retrieved
	 * @return options
	 */
	protected List<Option> getFragmentOptions(Concept question) {
		List<Option> ret = new ArrayList<Option>();
		for (ConceptAnswer candidate : question.getAnswers()) {
			Option option = new Option(candidate.getAnswerConcept().getName().getName(), null);
			ret.add(option);
		}
		return ret;
	}
	
	/**
	 * Sets required fragment parameters
	 * 
	 * @param context
	 */
	protected void initializeFragment(FormEntryContext context) {
		if (concept.getDatatype().isCoded()) {
			// Chances are, the associated fragment must dealing with options if concept datatype is coded
			List<Option> options = getFragmentOptions(concept);
			if (options != null) {
				fragmentParams.put("options", options);
			}
		}
		if (FormEntryContext.Mode.EDIT == context.getMode() || FormEntryContext.Mode.VIEW == context.getMode()) {	
			Encounter existingEncounter = context.getExistingEncounter();
			if (existingEncounter != null) {
				// The existingEncounter usually has stale references; pull a clean copy from the database
				Encounter encounter = Context.getEncounterService().getEncounter(existingEncounter.getEncounterId());
				for (Obs candidate : encounter.getAllObs()) {
					if (candidate.getConcept().equals(concept) && candidate.getComment().equals(getFormFieldName())) {
						Object initialValue = getObsValue(candidate);
						if (initialValue != null) {
							fragmentParams.put(initFragmentParamName, initialValue);
						}
					}
				}
			} 
		}
		if (fragmentParams.containsKey("classes")) {
			String classes = (String) fragmentParams.get("classes");
			fragmentParams.replace("classes", Arrays.asList(classes.split(",")));
		}
	}
	
	/**
	 * Returns the {@link Obs}'s value depending on the associated {@link Concept}'s datatype.
	 * 
	 * @param obs
	 */
	protected Object getObsValue(Obs obs) {
		ConceptDatatype datatype = obs.getConcept().getDatatype();
		if (datatype.isNumeric()) {
			return obs.getValueNumeric();
		}
		if (datatype.isBoolean()) {
			return obs.getValueBoolean();
		}
		if (datatype.isDate()) {
			return obs.getValueDate();
		}
		if (datatype.isDateTime()) {
			return obs.getValueDatetime();
		}
		if (datatype.isCoded()) {
			// the name of the selected answer
			return obs.getValueCoded().getName().getName();
		}
		if (datatype.isText()) {
			return obs.getValueText();
		}
		throw new RuntimeException("Unsupported concept datatype: " + datatype.getName());
	}
	
	private String getFormFieldName() {
		return (String) fragmentParams.get("formFieldName");
	}
	
	public UiUtils getUiUtils() {
		return uiUtils;
	}

	public void setUiUtils(UiUtils uiUtils) {
		this.uiUtils = uiUtils;
	}

	public Map<String, Object> getFragmentParams() {
		return fragmentParams;
	}

	public void setFragmentParams(Map<String, Object> fragmentParams) {
		this.fragmentParams = fragmentParams;
	}

	public Concept getConcept() {
		return concept;
	}

	public void setConcept(Concept concept) {
		this.concept = concept;
	}

	public String getViewProvider() {
		return viewProvider;
	}

	public void setViewProvider(String viewProvider) {
		this.viewProvider = viewProvider;
	}

	public String getFragment() {
		return fragment;
	}

	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	public String getInitFragmentParamName() {
		return initFragmentParamName;
	}

	public void setInitFragmentParamName(String initFragmentParamName) {
		this.initFragmentParamName = initFragmentParamName;
	}
	
	public static class Option {
		
		private String value;
		
		// Applies to radiobuttons
		private boolean checked;
		
		// Applies to dropdown widgets
		private boolean selected;
		
		private String label;
		
		public Option() {
			
		}
		
		public Option(String value, String label) {
			if (StringUtils.isNotBlank(value)) {
				this.value = value;
			}
			if (StringUtils.isBlank(label)) {
				this.label = value;
			} else {
				this.label = label;
			}
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}
	}
	
}
