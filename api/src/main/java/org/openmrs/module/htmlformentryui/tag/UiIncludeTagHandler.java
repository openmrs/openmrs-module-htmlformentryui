package org.openmrs.module.htmlformentryui.tag;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.handler.AbstractTagHandler;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageAction;
import org.w3c.dom.Node;

/**
 * Usage:
 * * <uiInclude provider="yourModuleId" javascript="fileAndPath.js" [priority="5"] />
 * * <uiInclude provider="yourModuleId" css="fileAndPath.css" [priority="10"] />
 * * <uiInclude provider="yourModuleId" fragment="pathAndFragmentName" fragmentParams="param1=value&param2=value"/>
 *
 * @see org.openmrs.ui.framework.ResourceIncluder
 * @see UiUtils#includeJavascript(String, String, Integer)
 * @see UiUtils#includeCss(String, String, Integer)
 */
public class UiIncludeTagHandler extends AbstractTagHandler {
	
	private Log log = LogFactory.getLog(getClass());
	
	
    @Override
    public boolean doStartTag(FormEntrySession formEntrySession, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
		
        UiUtils uiUtils = (UiUtils) formEntrySession.getAttribute("uiUtils");
        if (uiUtils == null) {
            throw new IllegalArgumentException("Cannot use " + node.getNodeName() + " tag if no UiUtils object is available");
        }

        String provider = getAttribute(node, "provider", null);
        if (provider == null) {
            throw new IllegalArgumentException(node.getNodeName() + " tag requires a provider attribute");
        }

        Integer priority = null;
        String temp = getAttribute(node, "priority", null);
        if (StringUtils.isNotBlank(temp)) {
            priority = Integer.valueOf(temp);
        }

        String js = getAttribute(node, "javascript", null);
        if (StringUtils.isNotEmpty(js)) {
            try {
                uiUtils.includeJavascript(provider, js, priority);
            }
            catch (NullPointerException e) {
                // there will be a NPE if the form is processed via FragmentAction (which can happen when viewing or submitting a form)
                // because FragmentAction does not contain a resource Includer; we want to fail soft in this case
                // (the takeaway here for form developers is that resources included via uiInclude may not be available when viewing a form)
            }
        }

        String css = getAttribute(node, "css", null);
        if (StringUtils.isNotEmpty(css)) {
            try {
                uiUtils.includeCss(provider, css, priority);
            }
            catch (NullPointerException e) {
                // see note above in previous NPE catch block
            }
        }
        
        doIncludeFragment(node, uiUtils, formEntrySession, out, provider);
        
        return false;
    }

    @Override
    public void doEndTag(FormEntrySession formEntrySession, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        // do nothing
    }
	
    /**
     * Includes a fragment to an HtmlForm.
     * 
     * @should include a given fragment to an HtmlForm
     * @should pickup fragment parameters from the {@code fragmentParams} attribute
     * @should also pickup fragment parameters from the {@code fragment} attribute as {@code URL} parameters
     * @param node HtmlForm point at which this fragment is included
     * @param uiUtils ui-framework API used to include the fragment
     * @param formEntrySession this HtmlForm session
     * @param provider fragment Provider
     */
    protected void doIncludeFragment(Node node, UiUtils uiUtils, FormEntrySession formEntrySession, PrintWriter out, String provider) {
    	String fragment = getAttribute(node, "fragment", null);
        if (StringUtils.isNotEmpty(fragment)) {
        	Map<String, Object> fragmentConfig = new HashMap<String, Object>();
        	try {
        		URI fragmentUrl = new URI(fragment);
	        	if (fragmentUrl.getQuery() != null) {
	        		fragmentConfig.putAll(paramsToMap(formEntrySession.evaluateVelocityExpression(fragment)));
	        	}
	        	
	        	String fragmentParams = getAttribute(node, "fragmentParams", null);
	        	if (StringUtils.isNotEmpty(fragmentParams)) {
	        		StringBuilder urlBuilder = new StringBuilder();
	        		urlBuilder.append(fragmentUrl.getPath() + "?").append(fragmentParams);
	        		fragmentConfig.putAll(paramsToMap(formEntrySession.evaluateVelocityExpression(urlBuilder.toString())));
	        	}
        	} catch(URISyntaxException e) {
        		log.error("Invalid fragment URL", e);
        	} 
        	
            try {
				if (fragmentConfig.isEmpty()) {
					out.print(uiUtils.includeFragment(provider, fragment));
				} else {
					out.print(uiUtils.includeFragment(provider, fragment, fragmentConfig));
				}
				
            } catch (PageAction pageAction) {
                throw new IllegalStateException("Tried to include a fragment that threw a PageAction", pageAction);
            }
            catch (NullPointerException e) {
                // see note above in previous NPE catch block
            }
        }

    }
    
    /**
     * Parses urlParameters to a {@link Map} of fragment configurations.
     * 
     * @param queryString url string.
     */
	protected Map<String, Object> paramsToMap(String queryString) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
    	    List<NameValuePair> prams = URLEncodedUtils.parse(new URI(queryString), "UTF-8");
    		for(NameValuePair param : prams) {
    			map.put(param.getName(), param.getValue());
    		}
    	} catch (URISyntaxException e) {
    		log.error("Invalid fragment URL", e);
    	}
		return map;
	}
}