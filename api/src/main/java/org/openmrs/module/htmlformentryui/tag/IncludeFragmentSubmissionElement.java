package org.openmrs.module.htmlformentryui.tag;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.InvalidActionException;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.HtmlGeneratorElement;
import org.openmrs.module.htmlformentryui.constant.TagsConstant;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageAction;

public class IncludeFragmentSubmissionElement implements HtmlGeneratorElement, FormSubmissionControllerAction {
	
	private static final Logger log = Logger.getLogger(IncludeFragmentSubmissionElement.class);
	
	String groupingConceptId = null;
	
	String questionConceptId = null;
	
	String id = null;
	
	String name = null;
	
	ConceptService conceptService = null;
	
	String providerName = null;
	
	UiUtils uiUtils = null;
	
	String fragmentId = null;
	
	String additionalParam = null;
	
	String placeholder = null;
	
	private String paramConceptId = null;
	
	private Map<String, Object> config = new HashMap<String, Object>();
	
	private Concept paramConcept = null;
	
	Obs childObs = null;
	
	private String classes = null;
	
	private Encounter encounter = null;
	
	Map<Obs, Set<Obs>> existingObsInGroups = null;
	
	Map<Concept, List<Obs>> existingObs = null;
	
	private List<Obs> childObsList = new ArrayList<Obs>();
	
	private Patient patient;
	
	private List<Obs> groupObsList = new ArrayList<Obs>();
	
	private void setInstanceVariables(FormEntrySession session, Map<String, String> parameters, ConceptService conceptService) {
		this.conceptService = conceptService;
		this.providerName = parameters.get(TagsConstant.Params.HTMLFORMENTRY_PARAMS_PROVIDERS);
		this.fragmentId = parameters.get(TagsConstant.Params.HTMLFORMENTRY_PARAMS_FRAGMENTID);
		this.id = parameters.get(TagsConstant.Params.HTMLFORMENTRY_PARAMS_ID);
		this.additionalParam = parameters.get(TagsConstant.Params.HTMLFORMENTRY_PARAMS_ADDITIONAL);
		this.placeholder = parameters.get(TagsConstant.Params.HTMLFORMENTRY_PARAMS_PLACEHOLDER);
		this.groupingConceptId = parameters.get(TagsConstant.ElementLiterals.PARAMETER_GROUPING_CONCEPT_UUID);
		this.paramConceptId = parameters.get(TagsConstant.Params.HTMLFORMENTRY_PARAMS_CONCEPT);
		this.classes = parameters.get(TagsConstant.Params.HTMLFORMENTRY_PARAMS_CLASSES);
		this.classes = StringUtils.isEmpty(classes) ? "" : classes;
		this.paramConcept = HtmlFormEntryUtil.getConcept(paramConceptId);
		this.encounter = session.getEncounter();
		this.patient = session.getPatient();
		config.put("id", id);
		setConfigParamater(additionalParam);
		setObservation(session.getContext(), encounter);
	}
	
	private void setObservation(FormEntryContext context, Encounter encounter) {
		if (encounter != null) {
			context.setupExistingData(encounter);
			if (groupingConceptId != null) {
				setExistingObsInGroups(context);
			} else {
				setExistingObs(context);
			}
		}
	}
	
	private void setConfigParamater(String additionalParam) {
		String[] splitKeyValues = additionalParam.split(";");
		for (String keyValues : splitKeyValues) {
			String[] kValue = keyValues.split(":");
			String key = kValue[0];
			String values = kValue[1];
			if (values.contains(",")) {
				String[] splitValues = values.split(",");
				String[] valueArr = new String[splitValues.length];
				for (int i = 0; i < splitValues.length; i++) {
					valueArr[i] = splitValues[i];
				}
				config.put(key, valueArr);
			} else {
				config.put(key, values);
			}
		}
	}
	
	
	
	public IncludeFragmentSubmissionElement(FormEntrySession session, Map<String, String> parameters,ConceptService conceptService) {
		uiUtils = (UiUtils) session.getAttribute("uiUtils");
		if (uiUtils == null) {
			throw new IllegalArgumentException("Cannot includeFragment tag if no UiUtils object is available");
		}
		setInstanceVariables(session, parameters, conceptService);
	}
	
	@Override
	public String generateHtml(FormEntryContext context) {
		StringBuilder output = new StringBuilder();
		String initialValue = null;
		
		if (paramConceptId != null) {
			this.name = id + "-obs";
		} else {
			this.name = id;
		}
		
		if (paramConceptId == null && groupingConceptId == null) {
			try {
				output.append(uiUtils.includeFragment(providerName, fragmentId, config));
			}
			catch (PageAction e) {
				log.info(e.getMessage());
			}
			catch (NullPointerException ex) {
				// DARIUS: if we are validating/submitting the form, then this method is being
				// called from a fragment action method
				// and the UiUtils we have access to doesn't have a FragmentIncluder. That's
				// okay, because we don't actually
				// need to generate the HTML, so we can pass on this exception.
				// (This is hacky, but I don't see a better way to do it.)
				return "Submitting the form, so we don't generate HTML";
			}
		} else if (context.getMode().equals(Mode.VIEW)) {
			try {
				if ((context.getCurrentObsGroupConcepts() != null && context.getCurrentObsGroupConcepts().size() > 0)
				        && existingObsInGroups != null) {
					List<Obs> existingObsFromGroup = findGroupObs(context.getCurrentObsGroupConcepts().get(0));
					if (!existingObsFromGroup.isEmpty()) {
						childObs = getObsFromGroup(existingObsFromGroup, paramConcept);
						if (childObs != null) {
							initialValue = getObsValue(childObs, paramConcept);
						}
					}
				} else if (existingObs != null && existingObs.get(paramConcept) != null
				        && existingObs.get(paramConcept).size() > 0) {
					childObs = existingObs.get(paramConcept).get(0);
					if (childObs != null) {
						initialValue = getObsValue(childObs, paramConcept);
					}
				}
				output.append(TagsConstant.ElementLiterals.HTMLFORMENTRY_SPAN_TYPE_TEXT);
			}
			catch (NullPointerException ex) {
				// DARIUS: if we are validating/submitting the form, then this method is being
				// called from a fragment action method
				// and the UiUtils we have access to doesn't have a FragmentIncluder. That's
				// okay, because we don't actually
				// need to generate the HTML, so we can pass on this exception.
				// (This is hacky, but I don't see a better way to do it.)
				return "Submitting the form, so we don't generate HTML";
			}
			
		} else {
			try {
				if (context.getCurrentObsGroupConcepts() != null && context.getCurrentObsGroupConcepts().size() > 0
				        && existingObsInGroups != null) {
					List<Obs> existingObsFromGroup = findGroupObs(context.getCurrentObsGroupConcepts().get(0));
					if (!existingObsFromGroup.isEmpty()) {
						childObs = getObsFromGroup(existingObsFromGroup, paramConcept);
						if (childObs != null) {
							initialValue = getObsValue(childObs, paramConcept);
						}
					}
				} else if (existingObs != null && existingObs.get(paramConcept) != null
				        && existingObs.get(paramConcept).size() > 0) {
					childObs = existingObs.get(paramConcept).get(0);
					if (childObs != null) {
						initialValue = getObsValue(childObs, paramConcept);
					}
				}
				
				output.append(TagsConstant.ElementLiterals.HTMLFORMENTRY_INPUT_TYPE_TEXT);
				output.append(uiUtils.includeFragment(providerName, fragmentId, config));
			}
			catch (PageAction e) {
				log.info(e.getMessage());
			}
			catch (NullPointerException ex) {
				// DARIUS: if we are validating/submitting the form, then this method is being
				// called from a fragment action method
				// and the UiUtils we have access to doesn't have a FragmentIncluder. That's
				// okay, because we don't actually
				// need to generate the HTML, so we can pass on this exception.
				// (This is hacky, but I don't see a better way to do it.)
				return "Submitting the form, so we don't generate HTML";
			}
		}
		
		String template = output.toString();
		template = template.replace(TagsConstant.ElementLiterals.PLACEHOLDER_ID, id + "-obs");
		template = template.replace(TagsConstant.ElementLiterals.PLACEHOLDER_NAME, id + "-obs");
		template = template.replace(TagsConstant.ElementLiterals.SubstitutePlaceholder.CLASSES, classes);
		if (initialValue != null) {
			template = template.replace(TagsConstant.ElementLiterals.SubstitutePlaceholder.INITIAL_VALUE, initialValue);
		} else {
			template = template.replace(TagsConstant.ElementLiterals.SubstitutePlaceholder.INITIAL_VALUE, "");
		}
		return template;
	}
	
	@Override
	public Collection<FormSubmissionError> validateSubmission(FormEntryContext context, HttpServletRequest submission) {
		return null;
	}
	
	public String getObsValue(Obs childObs, Concept paramConcept) {
		String value;
		if (paramConcept.getDatatype().isDate()) {
			value = DateFormatUtils.format(childObs.getValueDate(), "dd/MM/yyyy") + "";
		} else
			value = childObs.getValueText();
		return value;
	}
	
	public Object updateValue(Concept paramConcept, Object value) {
		try {
			if (paramConcept.getDatatype().isDate()) {
				value = new SimpleDateFormat("dd/MM/yyyy").parse(value.toString());
			}
		}
		catch (ParseException p) {
			value = "";
		}
		return value;
	}
	
	@Override
	public void handleSubmission(FormEntrySession session, HttpServletRequest submission) {
		String value = submission.getParameter(name);
		Concept groupConcept = null;
		Obs group = null;
		
		if (StringUtils.isEmpty(paramConceptId) && StringUtils.isEmpty(groupingConceptId)) {
			return;
		}
		
		if (groupingConceptId != null) {
			groupConcept = HtmlFormEntryUtil.getConcept(groupingConceptId);
			group = new Obs(session.getPatient(), groupConcept, new Date(), session.getEncounter().getLocation());
		}
		Concept questionConcept = HtmlFormEntryUtil.getConcept(paramConceptId);
		try {
			if (childObs != null && session.getContext().getMode() == Mode.EDIT) {
				if (StringUtils.isNotBlank(value)) {
					session.getSubmissionActions().modifyObs(childObs, questionConcept, updateValue(paramConcept, value),
					    new Date(), null, "modified value");
				} else {
					session.getSubmissionActions().getObsToVoid().add(childObs);
				}
			} else if (groupingConceptId == null && session.getContext().getMode() == Mode.EDIT) {
				childObsList = Context.getObsService().getObservationsByPersonAndConcept(patient, paramConcept);
				boolean obsSaved = false;
				if (childObsList.size() > 0) {
					for (Obs ob : childObsList) {
						if (ob.getEncounter() == session.getEncounter()) {
							if (StringUtils.isNotBlank(value)) {
								session.getSubmissionActions().modifyObs(ob, questionConcept,
								    updateValue(paramConcept, value), new Date(), null, "modified value");
							} else {
								session.getSubmissionActions().getObsToVoid().add(ob);
							}
							obsSaved = true;
						}
					}
				}
				if (!obsSaved && StringUtils.isNotBlank(value)) {
					session.getSubmissionActions().createObs(paramConcept, updateValue(paramConcept, value), new Date(),
					    null);
				}
			} else {
				if (group != null) {
					session.getSubmissionActions().beginObsGroup(group);
				}
				if (questionConcept.getDatatype().isCoded()) {
					throw new RuntimeException(" concept coded not supported in fragement");
				} else {
					if (StringUtils.isNotBlank(value)) {
						session.getSubmissionActions().createObs(paramConcept, updateValue(paramConcept, value), new Date(),
						    null);
					}
					if (group != null) {
						session.getSubmissionActions().endObsGroup();
					}
				}
			}
		}
		catch (InvalidActionException e) {
			e.printStackTrace();
		}
	}
	
	public Obs getObsFromGroup(List<Obs> groupObsList, Concept obsConcept) {
		Obs captureObs = null;
		for (Obs groupObs : groupObsList) {
			Set<Obs> groupMembers = groupObs.getGroupMembers();
			for (Iterator<Obs> iterator = groupMembers.iterator(); iterator.hasNext();) {
				Obs obs = iterator.next();
				if (obsConcept.getConceptId().equals(obs.getConcept().getConceptId())) {
					captureObs = obs;
					break;
				}
			}
		}
		return captureObs;
	}
	
	private List<Obs> findGroupObs(Concept gropingConcept) {
		
		if (existingObsInGroups == null) {
			throw new RuntimeException("Grouping Concept not found");
		}
		Set<Obs> keySets = existingObsInGroups.keySet();
		for (Iterator<Obs> iterator = keySets.iterator(); iterator.hasNext();) {
			
			Obs obs = iterator.next();
			if (obs.getConcept().getConceptId().equals(gropingConcept.getConceptId())) {
				groupObsList.add(obs);
			}
		}
		return groupObsList;
	}
	
	private void setExistingObsInGroups(FormEntryContext context) {
		existingObsInGroups = context.getExistingObsInGroups();
	}
	
	private void setExistingObs(FormEntryContext context) {
		existingObs = context.getExistingObs();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	
}
