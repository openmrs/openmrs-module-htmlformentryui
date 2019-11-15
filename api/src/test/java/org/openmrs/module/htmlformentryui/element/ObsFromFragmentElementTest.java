package org.openmrs.module.htmlformentryui.element;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptName;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.api.ConceptNameType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionActions;
import org.openmrs.module.htmlformentryui.element.ObsFromFragmentElement.Option;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class ObsFromFragmentElementTest {
	
	@Mock
	private FormEntryContext context;
	
	@Mock
	private Encounter encounter;
	
	@Mock
	private Obs obs;
	
	@Mock
	private Concept concept;
	
	@Mock
	private MockHttpServletRequest request;
	
	@Mock
	private FormSubmissionActions submissionActions;
	
	@Mock
	private EncounterService encounterService;
	
	@Mock
	private ConceptService conceptService;

	@Mock
	private FormEntrySession session;
		
	private Map<String, Object> fragmentParams;
	
	private ObsFromFragmentElement element;
	
	private String formFieldName = "test-field";
		
	@Before
	public void setup() {
		mockStatic(Context.class);
		when(Context.getEncounterService()).thenReturn(encounterService);
		when(Context.getConceptService()).thenReturn(conceptService);
		when(encounterService.getEncounter(any(Integer.class))).thenReturn(encounter);
		when(session.getContext()).thenReturn(context);
		when(session.getSubmissionActions()).thenReturn(submissionActions);
		when(context.getExistingEncounter()).thenReturn(encounter);
		when(encounter.getAllObs()).thenReturn(Collections.singleton(obs));
		when(obs.getConcept()).thenReturn(concept);
		when(obs.getComment()).thenReturn(formFieldName);
		fragmentParams = new HashMap<String, Object>();
		fragmentParams.put("formFieldName", formFieldName);
		
		element = new ObsFromFragmentElement();
		element.setFragmentParams(fragmentParams);
		element.setInitFragmentParamName("initialValue");
	}
	
	@Test
	public void getObsValue_shouldSupportNumericConceptType() {
		// Setup 
		Concept concept = createConcept("Test Concept", ConceptDatatype.NUMERIC_UUID);
		when(obs.getConcept()).thenReturn(concept);
		when(obs.getValueNumeric()).thenReturn(50.0);
		
		// Replay
		Object value = element.getObsValue(obs);
		
		// verify
		Assert.assertEquals(50.0, value);
	}
	
	@Test
	public void getObsValue_shouldSupportBooleanConceptType() {
		// Setup 
		Concept concept = createConcept("Test Concept", ConceptDatatype.BOOLEAN_UUID);
		when(obs.getConcept()).thenReturn(concept);
		when(obs.getValueBoolean()).thenReturn(true);
		
		// Replay
		Object value = element.getObsValue(obs);
		
		// verify
		Assert.assertTrue((Boolean) value);
	}
	
	@Test
	public void getObsValue_shouldSupportDateConceptType() {
		// Setup 
		Date date = new GregorianCalendar(2014, Calendar.FEBRUARY, 11).getTime();
		Concept concept = createConcept("Test Concept", ConceptDatatype.DATE_UUID);
		when(obs.getConcept()).thenReturn(concept);
		when(obs.getValueDate()).thenReturn(date);
		
		// Replay
		Object value = element.getObsValue(obs);
		
		// verify
		Assert.assertEquals(date, value);
	}
	
	@Test
	public void getObsValue_shouldSupportCodedConceptType() {
		// Setup 
		Concept concept = createConcept("Test Concept", ConceptDatatype.CODED_UUID);
		Concept answer = mock(Concept.class);
		when(obs.getConcept()).thenReturn(concept);
		when(obs.getValueCoded()).thenReturn(answer);
		when(answer.getName()).thenReturn(createConceptName("Test Concept Answer", ConceptNameType.FULLY_SPECIFIED, false));
		
		// Replay
		Object value = element.getObsValue(obs);
		
		// verify
		Assert.assertEquals("Test Concept Answer", value);
	}
	
	@Test
	public void getOptions_shouldReturnOptionsFromTheAssociatedConcept() {
		// Setup
		Concept question = createMockedCodedConcept();
		
		// Replay
		List<Option> options = element.getFragmentOptions(question);
		
		// Verify
		Assert.assertTrue(options.size() == 2);
		Assert.assertEquals("Concept Answer 1", options.get(0).getValue());
		Assert.assertEquals("Concept Answer 2", options.get(1).getValue());
	}
	
	@Test
	public void initializeFragment_shouldSetOptionsParameterForCodedDatatypes() {
		// Setup
		element.setConcept(createMockedCodedConcept());
		
		// Replay 
		element.initializeFragment(context);
		
		// Verify
		List<Option> options = (List<Option>) element.getFragmentParams().get("options");	
		Assert.assertTrue(options.size() == 2);
		Assert.assertEquals("Concept Answer 1", options.get(0).getValue());
		Assert.assertEquals("Concept Answer 2", options.get(1).getValue());
	}
	
	@Test
	public void initializeFragment_shouldSetInitialValueWhileInViewOrEditMode() {
		// Setup
		when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		concept = createMockedCodedConcept();
		when(obs.getConcept()).thenReturn(concept);
		Concept option1 = concept.getAnswers().iterator().next().getAnswerConcept();
		when(obs.getValueCoded()).thenReturn(option1);
		element.setConcept(concept);
		
		// Replay 
		element.initializeFragment(context);
		
		// Verify
		String selectedOption = (String) element.getFragmentParams().get("initialValue");	
		Assert.assertEquals("Concept Answer 1", selectedOption);
	}
	
	@Test
	public void initializeFragment_shouldConvertClassesParamToList() {
		// Setup
		element.setConcept(createMockedCodedConcept());
		fragmentParams.put("classes", "required,form-control");
		
		// Replay 
		element.initializeFragment(context);
		
		// Verify
		List<String> classes = (List<String>) fragmentParams.get("classes");
		Assert.assertThat(classes.size(), is(2));
		Assert.assertThat(classes, containsInAnyOrder("required", "form-control"));
	}
	
	@Test
	public void handleSubmission_shouldCreateObsWithANumericConcept() {
		// Setup
		when(context.getMode()).thenReturn(FormEntryContext.Mode.ENTER);
		when(request.getParameter(formFieldName)).thenReturn("78");
		
		ConceptDatatype numericDatatype = new ConceptDatatype();
		numericDatatype.setUuid(ConceptDatatype.NUMERIC_UUID);
		when(concept.getDatatype()).thenReturn(numericDatatype);
		element.setConcept(concept);
		
		when(submissionActions.createObs(any(Concept.class), any(Object.class), any(Date.class), any(String.class))).thenAnswer(new Answer() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Concept concept = (Concept) invocation.getArguments()[0];
				Double valueNumeric = (Double) invocation.getArguments()[1];

				// Verify
				Assert.assertTrue(concept.getDatatype().isNumeric());
				Assert.assertThat(new Double(78), is(valueNumeric));
				return new Obs();
			}
		});
		
		// Replay
		element.handleSubmission(session, request);	
		
		// Verify
		verify(submissionActions, times(1)).createObs(any(Concept.class), any(Object.class), any(Date.class), any(String.class), any(String.class));
	}
	
	@Test
	public void handleSubmission_shouldCreateObsWithACodedConcept() {
		// Setup
		when(context.getMode()).thenReturn(FormEntryContext.Mode.ENTER);
		when(request.getParameter(formFieldName)).thenReturn("Concept Answer 2");
		element.setConcept(createMockedCodedConcept());
		
		when(submissionActions.createObs(any(Concept.class), any(Object.class), any(Date.class), any(String.class))).thenAnswer(new Answer() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Concept question = (Concept) invocation.getArguments()[0];
				Concept answer = (Concept) invocation.getArguments()[1];

				// Verify
				Assert.assertTrue(question.getDatatype().isCoded());
				Assert.assertEquals("Concept Answer 2", answer.getName().getName());
				return new Obs();
			}
		});
		
		// Replay
		element.handleSubmission(session, request);	
		
		// Verify
		verify(submissionActions, times(1)).createObs(any(Concept.class), any(Object.class), any(Date.class), any(String.class), any(String.class));

	}
	
	@Test
	public void handleSubmission_shouldCreateObsWithAConceptOfDatatypeDate() {
		// Setup
		when(context.getMode()).thenReturn(FormEntryContext.Mode.ENTER);
		when(request.getParameter(formFieldName)).thenReturn("2017-08-14");
		
		ConceptDatatype dateDatatype = new ConceptDatatype();
		dateDatatype.setUuid(ConceptDatatype.DATE_UUID);
		when(concept.getDatatype()).thenReturn(dateDatatype);
		element.setConcept(concept);
		  
		when(submissionActions.createObs(any(Concept.class), any(Object.class), any(Date.class), any(String.class), any(String.class))).thenAnswer(new Answer() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Date expectedDate = new SimpleDateFormat("yyyy-MM-dd").parse("2017-08-14");
				Concept concept = (Concept) invocation.getArguments()[0];
				Date valueDate = (Date) invocation.getArguments()[1];

				// Verify
				Assert.assertTrue(concept.getDatatype().isDate());
				Assert.assertEquals(expectedDate, valueDate);
				return new Obs();
			}
		});
		
		// Replay
		element.handleSubmission(session, request);	
		
		// Verify
		verify(submissionActions, times(1)).createObs(any(Concept.class), any(Object.class), any(Date.class), any(String.class), any(String.class));

	}
	
	@Test
	public void handleSubmission_shouldUpdateAnObsIfInEditMode() {
		// Setup
		when(context.getMode()).thenReturn(FormEntryContext.Mode.EDIT);
		when(context.removeExistingObs(concept)).thenReturn(Arrays.asList(obs));
		when(obs.getValueBoolean()).thenReturn(true);
		when(obs.getComment()).thenReturn(formFieldName);
		when(request.getParameter(formFieldName)).thenReturn("false");
		
		ConceptDatatype dateDatatype = new ConceptDatatype();
		dateDatatype.setUuid(ConceptDatatype.BOOLEAN_UUID);
		when(concept.getDatatype()).thenReturn(dateDatatype);
		element.setConcept(concept);
		
		doAnswer(new Answer() {

			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Obs existingObs = (Obs) invocation.getArguments()[0];
				Concept question = (Concept) invocation.getArguments()[1];
				Boolean newValueBoolean = (Boolean) invocation.getArguments()[2];

				// Verify
				Assert.assertTrue(question.getDatatype().isBoolean());
				Assert.assertTrue(existingObs.getValueBoolean());
				Assert.assertFalse(newValueBoolean);
				return null;
			}
			
		}).when(submissionActions).modifyObs(any(Obs.class), any(Concept.class), any(Object.class), any(Date.class), any(String.class), eq(formFieldName));
		
		// Replay
		element.handleSubmission(session, request);	
		
		// Verify
		verify(submissionActions, times(1)).modifyObs(any(Obs.class), any(Concept.class), any(Object.class), any(Date.class), any(String.class), eq(formFieldName));

	}
	
    @Test
	public void shouldEvaluateVelocityExpressions() throws Exception {
      // Setup
      Date date = new GregorianCalendar(2019, Calendar.SEPTEMBER, 16).getTime();
      when(conceptService.getConcept(5)).thenReturn(concept);
      session = new FormEntrySession(new Patient(), "xml", null);
      session.addToVelocityContext("date", date);
      HashMap<String, String> parameters = new HashMap<String, String>();
      parameters.put("provider", "uicommons");
      parameters.put("fragment", "field/datetimepicker");
      parameters.put("initFragmentParamName", "defaultDate");
      parameters.put("conceptId", "5");
      parameters.put("fragmentParams", "label=Field+Label;testDate=$date");
      
	  // Replay
	  ObsFromFragmentElement element = new ObsFromFragmentElement(parameters, null, session);
	  
	  // Verify
	  Object testDate = element.getFragmentParams().get("testDate");
	  Assert.assertEquals(date.toString(), testDate);
	  
	}
	    
	private Concept createMockedCodedConcept() {
		Concept question = mock(Concept.class);
		Concept answer1 = mock(Concept.class);
		ConceptAnswer conceptAnswer1 = new ConceptAnswer();
		conceptAnswer1.setAnswerConcept(answer1);
		Concept answer2 = mock(Concept.class);
		ConceptAnswer conceptAnswer2 = new ConceptAnswer();
		conceptAnswer2.setAnswerConcept(answer2);
		ConceptDatatype codedDatatype = new ConceptDatatype();
		codedDatatype.setUuid(ConceptDatatype.CODED_UUID);
		when(question.getAnswers()).thenReturn(Arrays.asList(conceptAnswer1, conceptAnswer2));
		when(question.getDatatype()).thenReturn(codedDatatype);
		when(answer1.getName()).thenReturn(createConceptName("Concept Answer 1", ConceptNameType.FULLY_SPECIFIED, false));
		when(answer2.getName()).thenReturn(createConceptName("Concept Answer 2", ConceptNameType.FULLY_SPECIFIED, false));
		
		return question;
	}
	
	/**
	 * Convenient factory method to create a populated Concept with a one fully specified name and
	 * one short name
	 */
	private Concept createConcept(String name, String datatypeUuid) {
		Concept concept = new Concept();
		ConceptDatatype datatype = new ConceptDatatype();
		datatype.setUuid(datatypeUuid);
		concept.setDatatype(datatype);
		concept.addName(createConceptName(name, ConceptNameType.FULLY_SPECIFIED, false));
		return concept;
	}
	
	/**
	 * Convenient factory method to create a populated Concept name.
	 */
	private ConceptName createConceptName(String name, ConceptNameType conceptNameType,
	        Boolean isLocalePreferred) {
		ConceptName result = new ConceptName();
		result.setName(name);
		result.setConceptNameType(conceptNameType);
		result.setLocalePreferred(isLocalePreferred);
		return result;
	}
}
