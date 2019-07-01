package org.openmrs.module.htmlformentryui.tag;

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

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.util.Map;

import org.apache.xerces.impl.xs.opti.DefaultNode;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class UiIcludeTagHandlerTest {
	
	private FormEntrySession session;
	private UiIncludeTagHandler handler;
	private Node node;
	
	private static final String PATIENT_UUID = "ea7ae6ac-e9de-430f-a5ce-4adcf3b1ed43";
	
	@Before
	public void setup() throws Exception {
		mockStatic(Context.class);
		when(Context.getAuthenticatedUser()).thenReturn(new User(1));
		Patient pat = new Patient();
		pat.setGender("M");
		pat.setUuid(PATIENT_UUID);
		Visit visit = new Visit();
		visit.setId(15);
		session = new FormEntrySession(pat, "xml", null);
		session.addToVelocityContext("visit", visit);
		handler = spy(new UiIncludeTagHandler());
		node = new NodeTest();
		
	}
	
	@Test
	public void parseFragmentParams_shouldParseFragmentUrlParametersIntoAMap() {
		Map<String, Object> props = handler.parseFragmentParams("path/page?age=5&gender=M", session);
		Assert.assertEquals(props.size(), 2);
		Assert.assertEquals("{gender=M, age=5}", props.toString());
	}
	
	@Test
	public void parseFragmentParams_shouldHandleVelocityExpressionsOnContextualOpenmrsObjects() {
		Map<String, Object> props = handler.parseFragmentParams("path/page?visitId=$visit.id&gender=$patient.gender&patientId=$patient.uuid", session);
		Assert.assertEquals("15", props.get("visitId"));
		Assert.assertEquals("M", props.get("gender"));
		Assert.assertEquals(PATIENT_UUID, props.get("patientId"));
	}
	
	@Test
	public void parseFragmentParams_shouldReturnEmptyWhenUrlHasNoParameters() {
		Map<String, Object> props = handler.parseFragmentParams("path/page", session);
		Assert.assertTrue(props.isEmpty());
	}
		
	@Test
	public void doIncludeFragment_shouldPickUpFragmentParameters() {
		// Something like : <uiInclude provider="provider" fragment="path/page" fragmentParams="retired=true&patientId=$patient.uuid" />
		doReturn("path/fragment").when(handler).getAttribute(node, "fragment", null);
		doReturn("retired=true&patientId=$patient.uuid").when(handler).getAttribute(node, "fragmentParams", null);
		
		handler.doIncludeFragment(node, null, session, null, null);
		verify(handler).parseFragmentParams("path/fragment?retired=true&patientId=$patient.uuid", session);
	}
	
	@Test
	public void doIncludeFragment_shouldPickUpFragmentParametersFromTheFragmentUrl() {
		// Something like : <uiInclude provider="provider" fragment="path/page?retired=true&patientId=$patient.uuid" />
		doReturn("path/fragment?retired=true&patientId=$patient.uuid").when(handler).getAttribute(node, "fragment", null);
		
		handler.doIncludeFragment(node, null, session, null, null);
		verify(handler).parseFragmentParams("path/fragment?retired=true&patientId=$patient.uuid", session);
	}
	
	/**
	 *  {@link Node} implementation for unit testing purposes
	 */
	private class NodeTest extends DefaultNode {
		
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
