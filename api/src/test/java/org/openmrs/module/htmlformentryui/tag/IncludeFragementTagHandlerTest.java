package org.openmrs.module.htmlformentryui.tag;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntryContext;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionActions;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentryui.constant.TagsConstant;
import org.openmrs.ui.framework.UiUtils;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class IncludeFragementTagHandlerTest {
	FormSubmissionActions action = null; 
	FormSubmissionController submissionController;
	FormEntrySession formEntrySession;
    FormEntryContext formEntryContext;
    IncludeFragmentTagHandler tagHandler;
    UiUtils uiUtils = null;
    ConceptService conceptService = null;
    IncludeFragmentSubmissionElement ifse= null;
    Context context = null; 
    private static String param ="classes:form-control,required;formFieldName:consultation_date;endDate:today";
    
    @Before
    public void setUp() throws Exception {
    	formEntrySession = mock(FormEntrySession.class);
        formEntryContext = mock(FormEntryContext.class);
        uiUtils = mock(UiUtils.class);
        conceptService = mock(ConceptService.class);
        ifse = mock(IncludeFragmentSubmissionElement.class);
        context = mock(Context.class);
        submissionController = mock(FormSubmissionController.class);
        
        tagHandler = new IncludeFragmentTagHandler();
        PowerMockito.mockStatic(Context.class);
        when(formEntrySession.getAttribute("uiUtils")).thenReturn(uiUtils);
        when(Context.getConceptService()).thenReturn(conceptService);
        when(formEntrySession.getSubmissionController()).thenReturn(submissionController);
        
    }
    
    @Test
    public void testGetSubstitutionWithNoArguments() throws Exception {
        Map<String, String> args = new HashMap<String, String>();
        args.put(TagsConstant.Params.HTMLFORMENTRY_PARAMS_ADDITIONAL, param);
        String substitution = tagHandler.getSubstitution(formEntrySession, null, args);
        Assert.assertEquals("null", substitution);
     }
    
}
