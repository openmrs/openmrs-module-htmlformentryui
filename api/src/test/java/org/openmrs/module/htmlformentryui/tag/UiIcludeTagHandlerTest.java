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
	public void paramsToMap_shouldParseFragmentUrlParametersIntoAMap() throws URISyntaxException {
		Map<String, Object> props = handler.paramsToMap("path/page?age=5&gender=M");
		Assert.assertEquals(props.size(), 2);
		Assert.assertEquals("{gender=M, age=5}", props.toString());
	}
	
	@Test
	public void paramsToMap_shouldReturnEmptyWhenUrlHasNoParameters() throws URISyntaxException {
		Map<String, Object> props = handler.paramsToMap("path/page");
		Assert.assertTrue(props.isEmpty());
	}
		
	@Test
	public void includeFragment_shouldPickUpFragmentParameters() throws URISyntaxException {
		// Something like : <uiInclude provider="provider" fragment="path/fragment" fragmentParams="retired=true&patientId=$patient.uuid" />
		doReturn("path/fragment").when(handler).getAttribute(node, "fragment", null);
		doReturn("retired=true&patientId=$patient.uuid").when(handler).getAttribute(node, "fragmentParams", null);
		
		handler.includeFragment(node, null, session, null, null);
		verify(handler).paramsToMap("path/fragment?retired=true&patientId=" + PATIENT_UUID);
	}
	
	@Test
	public void includeFragment_shouldPickUpFragmentParametersFromTheFragmentUrl() throws URISyntaxException {
		// Something like : <uiInclude provider="provider" fragment="path/fragment?retired=true&patientId=$patient.uuid" />
		doReturn("path/fragment?retired=true&visitId=$visit.id").when(handler).getAttribute(node, "fragment", null);
		
		handler.includeFragment(node, null, session, null, null);
		verify(handler).paramsToMap("path/fragment?retired=true&visitId=15");
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
