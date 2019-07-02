package org.openmrs.module.htmlformentryui.tag;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.net.URISyntaxException;
import java.util.Map;

import org.apache.xerces.impl.xs.opti.DefaultNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class UiIcludeTagHandlerTest {
	
	private FormEntrySession session;
	private UiIncludeTagHandler handler;
	private Node node;
	
	private static final String PATIENT_UUID = "ea7ae6ac-e9de-430f-a5ce-4adcf3b1ed43";
	private static final Integer VISIT_ID = 15;
	
	@Before
	public void setup() throws Exception {
		mockStatic(Context.class);
		when(Context.getAuthenticatedUser()).thenReturn(new User(1));
		handler = spy(new UiIncludeTagHandler());
		node = new TestNode();
		
		// setup htmlentrysession context
		Patient patient = new Patient();
		patient.setGender("M");
		patient.setUuid(PATIENT_UUID);
		Visit visit = new Visit();
		visit.setId(VISIT_ID);
		
		session = new FormEntrySession(patient, "xml", null);
		session.addToVelocityContext("visit", visit);
	}
	
	@Test
	public void paramsToMap_shouldParseFragmentUrlParametersIntoAMap() throws URISyntaxException {
		// replay
		Map<String, Object> map = handler.paramsToMap("path/page?age=5&gender=M");
		
		// verify
		Assert.assertEquals(map.size(), 2);
		Assert.assertEquals("{gender=M, age=5}", map.toString());
	}
	
	@Test
	public void paramsToMap_shouldReturnEmptyMapWhenUrlHasNoParameters() throws URISyntaxException {
		// replay
		Map<String, Object> map = handler.paramsToMap("path/page");
		
		// verify
		Assert.assertTrue(map.isEmpty());
	}
	
	@Test
	public void includeFragment_shouldEvaluateFragmentParameters() throws URISyntaxException {
		// setup
		doReturn("path/fragment").when(handler).getAttribute(node, "fragment", null);
		doReturn("retired=true&patientId=$patient.uuid").when(handler).getAttribute(node, "fragmentParams", null);
		
		// replay
		handler.includeFragment(node, null, session, null, null);
		
		// verify
		verify(handler).paramsToMap("path/fragment?retired=true&patientId=" + PATIENT_UUID);
	}
	
	@Test
	public void includeFragment_shouldEvaluateFragmentParametersWhenInFragmentUrl() throws URISyntaxException {
		// setup
		doReturn("path/fragment?retired=true&visitId=$visit.id").when(handler).getAttribute(node, "fragment", null);
		
		// replay
		handler.includeFragment(node, null, session, null, null);
		
		// verify
		verify(handler).paramsToMap("path/fragment?retired=true&visitId=" + VISIT_ID);
	}
	
	
	/**
	 *  {@link Node} implementation for unit testing purposes
	 */
	private class TestNode extends DefaultNode {
		
		@Override
		public NamedNodeMap getAttributes() {
	    	return new NamedNodeMapTest();
	    }
		
		private class NamedNodeMapTest implements NamedNodeMap {

			@Override
			public Node getNamedItem(String name) {
				return null;
			}

			@Override
			public Node setNamedItem(Node arg) throws DOMException {
				return null;
			}

			@Override
			public Node removeNamedItem(String name) throws DOMException {
				return null;
			}

			@Override
			public Node item(int index) {
				return null;
			}

			@Override
			public int getLength() {
				return 0;
			}

			@Override
			public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
				return null;
			}

			@Override
			public Node setNamedItemNS(Node arg) throws DOMException {
				return null;
			}

			@Override
			public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
				return null;
			}
			
		}
		
	}
	
}
