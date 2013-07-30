package org.openmrs.module.htmlformentryui.tag;

import org.apache.commons.lang.StringUtils;
import org.openmrs.module.htmlformentry.BadFormDesignException;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.handler.AbstractTagHandler;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.page.PageAction;
import org.w3c.dom.Node;

import java.io.PrintWriter;

/**
 * Usage:
 * * <uiInclude provider="yourModuleId" javascript="fileAndPath.js" [priority="5"] />
 * * <uiInclude provider="yourModuleId" css="fileAndPath.css" [priority="10"] />
 * * <uiInclude provider="yourModuleId" fragment="pathAndFragmentName" />
 *
 * @see org.openmrs.ui.framework.ResourceIncluder
 * @see UiUtils#includeJavascript(String, String, Integer)
 * @see UiUtils#includeCss(String, String, Integer)
 */
public class UiIncludeTagHandler extends AbstractTagHandler {

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
            uiUtils.includeJavascript(provider, js, priority);
        }

        String css = getAttribute(node, "css", null);
        if (StringUtils.isNotEmpty(css)) {
            uiUtils.includeCss(provider, css, priority);
        }

        String fragment = getAttribute(node, "fragment", null);
        if (StringUtils.isNotEmpty(fragment)) {
            try {
                out.print(uiUtils.includeFragment(provider, fragment));
            } catch (PageAction pageAction) {
                throw new IllegalStateException("Tried to include a fragment that threw a PageAction", pageAction);
            }
        }

        return false;
    }

    @Override
    public void doEndTag(FormEntrySession formEntrySession, PrintWriter out, Node parent, Node node) throws BadFormDesignException {
        // do nothing
    }

}