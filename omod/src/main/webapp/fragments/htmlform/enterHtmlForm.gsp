<%
    // config supports style (css style on div around form)
    // config supports cssClass (css class on div around form)

    // assumes jquery and jquery-ui from emr module
    ui.includeJavascript("htmlformentryui", "dwr-util.js")
    ui.includeJavascript("htmlformentryui", "htmlForm.js")
    ui.includeJavascript("uicommons", "emr.js")
    // TODO setup "confirm before navigating" functionality
%>

<script type="text/javascript" src="/${ contextPath }/moduleResources/htmlformentry/handlebars.min.js"></script>
<script type="text/javascript" src="/${ contextPath }/moduleResources/htmlformentry/htmlFormEntry.js"></script>
<script type="text/javascript" src="/${ contextPath }/moduleResources/htmlformentry/htmlForm.js"></script>
<link href="/${ contextPath }/moduleResources/htmlformentry/htmlFormEntry.css" type="text/css" rel="stylesheet" />

<script type="text/javascript">

    // for now we just expose these in the global scope for compatibility with htmlFormEntry.js and legacy forms
    function submitHtmlForm() {
        htmlForm.submitHtmlForm();
        return false;
    }

    function showDiv(id) {
        htmlForm.showDiv(id);
    }

    function hideDiv(id) {
        htmlForm.hideDiv(id);
    }

    function getValueIfLegal(idAndProperty) {
        htmlForm.getValueIfLegal(idAndProperty);
    }

    function loginThenSubmitHtmlForm() {
        html.loginThenSubmitHtmlForm();
    }

    var beforeSubmit = htmlForm.getBeforeSubmit();
    var beforeValidation = htmlForm.getBeforeValidation();
    var propertyAccessorInfo = htmlForm.getPropertyAccessorInfo();

    <% if (command.returnUrl) { %>
        htmlForm.setReturnUrl('${ command.returnUrl }');
    <% } %>

    jq(function() {

        // configure the encounter date widget
        // TODO this probably should be handled in HFE itself when configuring the widget? could handle this when implementing HTML-480?
        <% if (visit) { %>
            <% if (command.context.mode.toString().equals('ENTER') && !visit.isOpen()) { %>
                // set default date to the visit start date for retrospective visits
                htmlForm.setEncounterDate(new Date('${ visit.startDatetime }'));
            <% } %>

            // set valid date range based on visit
            htmlForm.setEncounterStartDateRange(new Date('${ visit.startDatetime }'));
            htmlForm.setEncounterStopDateRange(new Date('${ visit.stopDatetime ?: currentDatetime }'));

        <% } else { %>
            // note that we need to get the current datetime from the *server*, in case the server and client are in different time zones
            htmlForm.setEncounterStopDateRange(new Date('${ currentDatetime }'));
            htmlForm.setEncounterDate(new Date('${ currentDatetime }'));
        <% } %>

        // for now, just disable manual entry until we figure out proper validation
        htmlForm.disableEncounterDateManualEntry();

    });

	jq(document).ready(function() {
		jQuery.each(jq("htmlform").find('input'), function(){
		    jq(this).bind('keypress', function(e){
		       if (e.keyCode == 13) {
		       		if (!jq(this).hasClass("submitButton")) {
		       			e.preventDefault(); 
		       		}
		       }
		    });
		});
    });
    
</script>

<div id="${ config.id }" <% if (config.style) { %>style="${ config.style }"<% } %> <% if (config.cssClass) { %>class="${config.cssClass}"<% } %>>

    <span class="error" style="display: none" id="general-form-error"></span>

    <form id="htmlform" method="post" action="${ ui.actionLink("htmlformentryui", "htmlform/enterHtmlForm", "submit") }" onSubmit="submitHtmlForm(); return false;">
        <input type="hidden" name="personId" value="${ command.patient.personId }"/>
        <input type="hidden" name="htmlFormId" value="${ command.htmlFormId }"/>
        <input type="hidden" name="createVisit" value="${ createVisit }"/>
        <input type="hidden" name="formModifiedTimestamp" value="${ command.formModifiedTimestamp }"/>
        <input type="hidden" name="encounterModifiedTimestamp" value="${ command.encounterModifiedTimestamp }"/>
        <% if (command.encounter) { %>
        <input type="hidden" name="encounterId" value="${ command.encounter.encounterId }"/>
        <% } %>
        <% if (visit) { %>
        <input type="hidden" name="visitId" value="${ visit.visitId }"/>
        <% } %>
        <% if (command.returnUrl) { %>
        <input type="hidden" name="returnUrl" value="${ command.returnUrl }"/>
        <% } %>
        <input type="hidden" name="closeAfterSubmission" value="${ config.closeAfterSubmission }"/>

        ${ command.htmlToDisplay }

        <div id="passwordPopup" style="position: absolute; z-axis: 1; bottom: 25px; background-color: #ffff00; border: 2px black solid; display: none; padding: 10px">
            <center>
                <table>
                    <tr>
                        <td colspan="2"><b>${ ui.message("htmlformentry.loginAgainMessage") }</b></td>
                    </tr>
                    <tr>
                        <td align="right"><b>${ ui.message("emr.user.username") }:</b></td>
                        <td><input type="text" id="passwordPopupUsername"/></td>
                    </tr>
                    <tr>
                        <td align="right"><b>${ ui.message("emr.user.password") }:</b></td>
                        <td><input type="password" id="passwordPopupPassword"/></td>
                    </tr>
                    <tr>
                        <td colspan="2" align="center"><input type="button" value="Submit" onClick="loginThenSubmitHtmlForm()"/></td>
                    </tr>
                </table>
            </center>
        </div>
    </form>
</div>

<% if (command.fieldAccessorJavascript) { %>
<script type="text/javascript">
    ${ command.fieldAccessorJavascript }
</script>
<% } %>