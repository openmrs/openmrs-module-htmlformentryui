package org.openmrs.module.htmlformentryui.tag;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionActions;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentryui.constant.TagsConstant;
import org.openmrs.ui.framework.UiUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class IncludeFragmentSubmissionElementTest {
	FormSubmissionController submissionController;
	FormEntrySession formEntrySession;
    FormEntryContext formEntryContext;
    UiUtils uiUtils = null;
    ConceptService conceptService = null;
    IncludeFragmentSubmissionElement ifse= null;
    Context context = null; 
    private static String param ="classes:form-control,required;formFieldName:consultation_date;endDate:today";
    String provider="uicommon";
	String fragmentId="date";
	String name = null;
	Map<String, Object> config = new HashMap<String, Object>();
    @Before
    public void setUp() throws Exception {
    	formEntrySession = mock(FormEntrySession.class);
        formEntryContext = mock(FormEntryContext.class);
        uiUtils = mock(UiUtils.class);
        conceptService = mock(ConceptService.class);
        context = mock(Context.class);
        //submission = mock(HttpServletRequest.class);
        submissionController = mock(FormSubmissionController.class);
        PowerMockito.mockStatic(Context.class);
        config.put("formFieldName", "consultation_date");
        config.put("endDate", "today");
        String [] classes = new String [2];
        classes[0]="form-control";
        classes[1]="required";
        config.put("classes", "today");
        config.put("id", null);
        when(formEntrySession.getAttribute("uiUtils")).thenReturn(uiUtils);
        when(Context.getConceptService()).thenReturn(conceptService);
        when(formEntrySession.getSubmissionController()).thenReturn(submissionController);
        when(uiUtils.includeFragment(provider, fragmentId, config)).thenReturn("test");
        when(formEntrySession.getContext()).thenReturn(Mockito.mock(FormEntryContext.class));
        when(formEntrySession.getSubmissionActions()).thenReturn(mock(FormSubmissionActions.class));
    }
    
    
    @Test
    public void testIncludeFragmentSubmissionElement() throws Exception {
    	Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_ADDITIONAL, param);
        paramMap.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_PROVIDERS,provider);
        paramMap.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_FRAGMENTID,fragmentId);
        ifse = new IncludeFragmentSubmissionElement(formEntrySession,paramMap,conceptService);
        Assert.assertNotNull("constructor initialised successfully", ifse);
    }
    
    @Test
    public void testBeforeAfterHandleSubmission_shouldCallBeforeAfterHandlers() {
    	Map<String, String> paramMap = new HashMap<String, String>();
    	Concept concept = new Concept();
    	ConceptDatatype conceptDatatype = mock(ConceptDatatype.class);
    	concept.setDatatype(conceptDatatype);
    	when(concept.getDatatype().isCoded()).thenReturn(false);
    	when(HtmlFormEntryUtil.getConcept("1970642")).thenReturn(concept);
    	when(formEntrySession.getContext().getMode()).thenReturn(Mode.ENTER);
        paramMap.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_ADDITIONAL, param);
        paramMap.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_PROVIDERS,provider);
        paramMap.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_FRAGMENTID,fragmentId);
        paramMap.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_ID,"date_enrollment");
        paramMap.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_CONCEPT,"1970642");
    	ifse = new IncludeFragmentSubmissionElement(formEntrySession,paramMap,conceptService);
    	MockHttpServletRequest submission = new MockHttpServletRequest();
    	submission.addParameter("date_enrollment", "12-12-1992");
    	formEntryContext = new FormEntryContext(Mode.ENTER);
    	ifse.generateHtml(formEntryContext);
    	ifse.setName("date_enrollment");
    	ifse.handleSubmission(formEntrySession, submission);
        Assert.assertTrue(true);
        
    }
    
}
