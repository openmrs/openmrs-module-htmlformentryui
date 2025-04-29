package org.openmrs.module.htmlformentryui.tag;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.RegressionTestHelper;
import org.openmrs.module.htmlformentry.handler.SubstitutionTagHandler;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;

public class ObsFromFragmentTagTest extends BaseModuleContextSensitiveTest {
	
	@Before
	public void setup() throws Exception {
		executeDataSet("org/openmrs/module/htmlformentry/data/RegressionTest-data-openmrs-2.3.xml");
		HtmlFormEntryUtil.getService().addHandler("obsFromFragment",
		    Context.getRegisteredComponent("obsFromFragmentTagHandler", SubstitutionTagHandler.class));
	}
	
	@Test
	public void shouldHandleEditingOfSingleObsCorrectly() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "ObsFromFragment-withSingleObs";
			}
			
			@Override
			protected String getXmlDatasetPath() {
				return "org/openmrs/module/htmlformentryui/include/";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergy Date:" };
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// In the test context, the available UiUtils instance doesn't have a FragmentIncluder hence html will
				// not be generated to map the field name to value while handling submission.
				// (This is hacky, but I don't see a better way to do it.)
				request.addParameter("Allergy Date", "2014-02-11");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				results.assertProvider(502);
				results.assertLocation(2);
				
				Set<Obs> existingObs = results.getEncounterCreated().getAllObs(true);
				Assert.assertEquals(1, existingObs.size());
				Assert.assertEquals("2014-02-11", dateAsString(existingObs.iterator().next().getValueDatetime()));
				Assert.assertTrue(retainVoidedObs(existingObs).isEmpty());
			};
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Allergy Date:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// In the test context, the available UiUtils instance doesn't have a FragmentIncluder hence html will
				// not be generated to map the field name to value while handling submission.
				// (This is hacky, but I don't see a better way to do it.)
				request.addParameter("Allergy Date", "2015-02-11");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				Set<Obs> existingObs = results.getEncounterCreated().getObs();
				List<Obs> voidedObs = retainVoidedObs(results.getEncounterCreated().getAllObs(true));
				
				Assert.assertEquals(1, existingObs.size());
				Assert.assertEquals(1, voidedObs.size());
				
				// Verify new value
				Assert.assertEquals("2015-02-11", dateAsString(existingObs.iterator().next().getValueDatetime()));
				// Verify old value
				Assert.assertEquals("2014-02-11", dateAsString(voidedObs.get(0).getValueDatetime()));
			}
			
		}.run();
		
	}
	
	@Test
	public void shouldHandleEditingOfObsGroupMemberCorrectly() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "ObsFromFragment-withObsGroup";
			}
			
			@Override
			protected String getXmlDatasetPath() {
				return "org/openmrs/module/htmlformentryui/include/";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergy Date:" };
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				// In the test context, the available UiUtils instance doesn't have a FragmentIncluder hence html will
				// not be generated to map the field name to value while handling submission.
				// (This is hacky, but I don't see a better way to do it.)
				request.addParameter("Allergy Date", "2019-04-11");
			}
			
			@Override
			public void testResults(SubmissionResults results) {
				results.assertNoErrors();
				results.assertEncounterCreated();
				
				Set<Obs> existingObs = results.getEncounterCreated().getObs();
				Assert.assertEquals(1, existingObs.size());
				Assert.assertEquals("2019-04-11", dateAsString(existingObs.iterator().next().getValueDatetime()));
				Assert.assertTrue(retainVoidedObs(results.getEncounterCreated().getAllObs(true)).isEmpty());
			};
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Allergy Date:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				// In the test context, the available UiUtils instance doesn't have a FragmentIncluder hence html will
				// not be generated to map the field name to value while handling submission.
				// (This is hacky, but I don't see a better way to do it.)
				request.addParameter("Allergy Date", "2020-05-04");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				Set<Obs> existingObs = results.getEncounterCreated().getObs();
				List<Obs> voidedObs = retainVoidedObs(results.getEncounterCreated().getAllObs(true));
				
				Assert.assertEquals(1, existingObs.size());
				Assert.assertEquals(1, voidedObs.size());
				
				// Verify new value
				Assert.assertEquals("2020-05-04", dateAsString(existingObs.iterator().next().getValueDatetime()));
				// Verify old value
				Assert.assertEquals("2019-04-11", dateAsString(voidedObs.get(0).getValueDatetime()));
			}
			
		}.run();
	}
	
	@Test
	public void shouldVoidExistingObsWhenNewValueIsBlank() throws Exception {
		new RegressionTestHelper() {
			
			@Override
			public String getFormName() {
				return "ObsFromFragment-withObsGroup";
			}
			
			@Override
			protected String getXmlDatasetPath() {
				return "org/openmrs/module/htmlformentryui/include/";
			}
			
			@Override
			public String[] widgetLabels() {
				return new String[] { "Date:", "Location:", "Provider:", "Allergy Date:" };
			}
			
			@Override
			public boolean doEditEncounter() {
				return true;
			}
			
			@Override
			public void setupRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter(widgets.get("Date:"), dateAsString(new Date()));
				request.addParameter(widgets.get("Location:"), "2");
				request.addParameter(widgets.get("Provider:"), "502");
				request.addParameter("Allergy Date", "2020-06-10");
			}
			
			@Override
			public String[] widgetLabelsForEdit() {
				return new String[] { "Allergy Date:" };
			}
			
			@Override
			public void setupEditRequest(MockHttpServletRequest request, Map<String, String> widgets) {
				request.addParameter("Allergy Date", " ");
			}
			
			@Override
			public void testEditedResults(SubmissionResults results) {
				Set<Obs> existingObs = results.getEncounterCreated().getObs();
				List<Obs> voidedObs = retainVoidedObs(results.getEncounterCreated().getAllObs(true));
				
				// Verify old value was voided
				Assert.assertEquals(0, existingObs.size());
				Assert.assertEquals("Both Obs group and Allergy Date should be voided", 2, voidedObs.size());
				
				// Verify old value
				for (Obs o : voidedObs) {
					if (o.getValueDatetime() != null) {
						Assert.assertEquals("2020-06-10", dateAsString(o.getValueDatetime()));
					}
				}
			}
			
		}.run();
	}
	
	private List<Obs> retainVoidedObs(Set<Obs> mixedObs) {
		List<Obs> voidedObs = new ArrayList<Obs>();
		for (Obs candidate : mixedObs) {
			if (candidate.isVoided()) {
				voidedObs.add(candidate);
			}
		}
		return voidedObs;
	}
	
}
