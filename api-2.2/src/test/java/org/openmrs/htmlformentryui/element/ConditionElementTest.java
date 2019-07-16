package org.openmrs.htmlformentryui.element;

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
import org.openmrs.api.ConceptService;
import org.openmrs.api.ConditionService;
import org.openmrs.api.context.Context;
import org.openmrs.htmlformentryui.ConditionElement;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
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
	
	@Mock
    private MessageSourceService messageSourceService;
	@Mock
	private ConceptService conceptService;
	@Mock
	private ConditionService conditionService;
	@Mock
	private FormEntrySession session;
	@Mock
	private FormEntryContext context;
	@Mock
	private ConceptSearchAutocompleteWidget conceptAutocomplete;
	@Mock
	private RadioButtonsWidget conditionStatusWidget;
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

		// Setup html form session context
		when(context.getMode()).thenReturn(Mode.ENTER);
		request = new MockHttpServletRequest();
		request.addParameter(CONDITION_NAME_WIDGET_ID, NONE_CODED_CONCEPT);
		
		when(session.getContext()).thenReturn(context);
		when(session.getPatient()).thenReturn(new Patient(1));
		
		// Stub widgets
		when(conceptAutocomplete.getValue(context, request)).thenReturn("1519");
		when(conditionStatusWidget.getValue(context, request)).thenReturn("active");
		when(onsetDate.getValue(context, request)).thenReturn(new GregorianCalendar(2014, Calendar.FEBRUARY, 11).getTime());
		when(endDate.getValue(context, request)).thenReturn(new Date());
		element = spy(ConditionElement.class);
				
		// inject mocks
		element.setConditionName(conceptAutocomplete);
		element.setConditionStatusWidget(conditionStatusWidget);
		element.setOnSetDate(onsetDate);
		element.setEndDate(endDate);
		
	}
	
	@Test
	public void handleSubmission_shouldCreateNewCondition() {
		// replay
		element.handleSubmission(session, request);
		
		// verify
		ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
		verify(conditionService).saveCondition(captor.capture());
		Condition condition = captor.getValue();
		Assert.assertEquals(ConditionClinicalStatus.ACTIVE, condition.getClinicalStatus());
		Assert.assertNotNull(condition.getPatient());
		Assert.assertNotNull(condition.getCondition());		
		
	}
	
	@Test
	public void handleSubmission_shouldCreateInactiveCondition() {
		// setup
		when(conditionStatusWidget.getValue(context, request)).thenReturn("inactive");
		
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
		when(context.getFieldName(conceptAutocomplete)).thenReturn(CONDITION_NAME_WIDGET_ID);
		when(conceptAutocomplete.getValue(context, request)).thenReturn("");
		
		
		// replay
		element.handleSubmission(session, request);
		
		// verify
		ArgumentCaptor<Condition> captor = ArgumentCaptor.forClass(Condition.class);
		verify(conditionService).saveCondition(captor.capture());
		Condition condition = captor.getValue();
		Assert.assertEquals(NONE_CODED_CONCEPT, condition.getCondition().getNonCoded());

	}

}
