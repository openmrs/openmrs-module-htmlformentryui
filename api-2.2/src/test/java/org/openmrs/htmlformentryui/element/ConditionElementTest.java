package org.openmrs.htmlformentryui.element;

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.hamcrest.CoreMatchers.is;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openmrs.Condition;
import org.openmrs.ConditionClinicalStatus;
import org.openmrs.Patient;
import org.openmrs.api.ConditionService;
import org.openmrs.api.context.Context;
import org.openmrs.htmlformentryui.ConditionElement;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionError;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.widget.ConceptSearchAutocompleteWidget;
import org.openmrs.module.htmlformentry.widget.DateWidget;
import org.openmrs.module.htmlformentry.widget.RadioButtonsWidget;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;


@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class ConditionElementTest {
	
	private ConditionElement element;
	private MockHttpServletRequest request;
	private final String CONDITION_NAME_WIDGET_ID = "condition-name";
	private final String NONE_CODED_CONCEPT = "Test None Coded Concept";
	private final String CONDITION_REQUIRED_ERROR = "Condition required";
	private final String END_DATE_BEFORE_ONSET_DATE_ERROR = "End date can't be before onset date";
	@Mock
    private MessageSourceService messageSourceService;
	@Mock
	private ConditionService conditionService;
	@Mock
	private FormEntrySession session;
	@Mock
	private FormEntryContext context;
	@Mock
	private ConceptSearchAutocompleteWidget conditionSearchWidget;
	@Mock
	private RadioButtonsWidget conditionStatusesWidget;
	@Mock
	private DateWidget endDate;
	@Mock
	private DateWidget onsetDate;

	@Before
	public void setup() {
		// Stub services
		when(conditionService.saveCondition(any(Condition.class))).thenAnswer(new Answer<Condition>() {
			  @Override
			  public Condition answer(InvocationOnMock invocation) throws Throwable {
			    return (Condition) invocation.getArguments()[0];
			  }
			});
		
		mockStatic(Context.class);
		when(Context.getConditionService()).thenReturn(conditionService);
		when(Context.getMessageSourceService()).thenReturn(messageSourceService);
		
		// Setup html form session context
		when(context.getMode()).thenReturn(Mode.ENTER);
		request = new MockHttpServletRequest();
		when(session.getContext()).thenReturn(context);
		when(session.getPatient()).thenReturn(new Patient(1));
		
		when(onsetDate.getValue(context, request)).thenReturn(new GregorianCalendar(2014, Calendar.FEBRUARY, 11).getTime());
		
		// setup condition element
		element = spy(ConditionElement.class);
		element.setConditionSearch(conditionSearchWidget);
		element.setConditionStatusesWidget(conditionStatusesWidget);
		element.setOnSetDate(onsetDate);
		element.setEndDate(endDate);
		
	}
	
	@Test
	public void handleSubmission_shouldCreateNewCondition() {
		// setup
		when(conditionSearchWidget.getValue(context, request)).thenReturn("1519");
		when(conditionStatusesWidget.getValue(context, request)).thenReturn("active");
		
		// replay
		element.handleSubmission(session, request);
		
		// verify
		ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
		verify(conditionService).saveCondition(captor.capture());
		Condition condition = captor.getValue();
		Assert.assertEquals(ConditionClinicalStatus.ACTIVE, condition.getClinicalStatus());
		Assert.assertThat(condition.getCondition().getCoded().getId(), is(1519));
		
	}
	
	@Test
	public void handleSubmission_shouldCreateInactiveCondition() {
		// setup
		when(endDate.getValue(context, request)).thenReturn(new GregorianCalendar(2018, Calendar.DECEMBER, 1).getTime());
		when(conditionStatusesWidget.getValue(context, request)).thenReturn("inactive");
		
		// replay
		element.handleSubmission(session, request);
		
		// verify
		ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
		verify(conditionService).saveCondition(captor.capture());
		Condition condition = captor.getValue();
		Assert.assertEquals(ConditionClinicalStatus.INACTIVE, condition.getClinicalStatus());

	}
	
	@Test
	public void handleSubmission_shouldSupportNoneCodedConceptValues() {
		// setup
		request.addParameter(CONDITION_NAME_WIDGET_ID, NONE_CODED_CONCEPT);
		when(context.getFieldName(conditionSearchWidget)).thenReturn(CONDITION_NAME_WIDGET_ID);
		when(conditionSearchWidget.getValue(context, request)).thenReturn("");
		
		// replay
		element.handleSubmission(session, request);
		
		// verify
		ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
		verify(conditionService).saveCondition(captor.capture());
		Condition condition = captor.getValue();
		Assert.assertEquals(NONE_CODED_CONCEPT, condition.getCondition().getNonCoded());

	}
	
	@Test
	public void handleSubmission_shouldNotCreateConditionInViewMode() {
		// setup
		when(context.getMode()).thenReturn(Mode.VIEW);
		
		// replay
		element.handleSubmission(session, request);

		// verify
		verify(conditionService, never()).saveCondition(any(Condition.class));

	}

	@Test
	public void validateSubmission_shouldFailValidationIfConditionIsNotGivenButRequired() {
		// setup
		element.setRequired(true);
		when(conditionSearchWidget.getValue(context, request)).thenReturn(null);
		when(messageSourceService.getMessage("htmlformentryui.conditionui.condition.required")).thenReturn(CONDITION_REQUIRED_ERROR);

		// replay
		List<FormSubmissionError> errors = (List<FormSubmissionError>) element.validateSubmission(context, request);
		
		// verify
		Assert.assertEquals(CONDITION_REQUIRED_ERROR, errors.get(0).getError());
		
	}
	
	@Test
	public void validateSubmission_shouldFailValidationIfOnsetDateIsGreaterThanEnddate() {
		// setup
		when(endDate.getValue(context, request)).thenReturn(new GregorianCalendar(2012, Calendar.DECEMBER, 8).getTime());
		when(messageSourceService.getMessage("htmlformentryui.conditionui.endDate.before.onsetDate.error")).thenReturn(END_DATE_BEFORE_ONSET_DATE_ERROR);
		
		// replay
		List<FormSubmissionError> errors = (List<FormSubmissionError>) element.validateSubmission(context, request);
		
		// verify
		Assert.assertEquals(END_DATE_BEFORE_ONSET_DATE_ERROR, errors.get(0).getError());
		
	}
}
