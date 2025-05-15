<%
    ui.decorateWith("appui", "standardEmrPage")

    ui.includeJavascript("htmlformentryui", "htmlform/viewEncounterWithHtmlForm.js")
    ui.includeCss("htmlformentryui", "htmlform/viewEncounterWithHtmlForm.css")
    ui.includeJavascript("uicommons", "moment-with-locales.min.js")
%>

<link href="/${ contextPath }/moduleResources/htmlformentry/orderWidget.css" type="text/css" rel="stylesheet" />
<script type="text/javascript" src="/${ contextPath }/moduleResources/htmlformentry/orderWidget.js"></script>

<script type="text/javascript">
    window.viewHtmlForm = {
        encounterId: ${ encounter.id },
        returnUrl: "${ ui.encodeJavaScript(returnUrl) }",
        htmlFormId: ${htmlForm.id},
        datetimeFormat:"${ui.getJSDatetimeFormat()}",
        dateFormat:"${ui.getJSDateFormat()}",
        locale:"${ui.getLocale()}"
    };

    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.encodeJavaScript(returnLabel) }", link: "${ ui.encodeJavaScript(returnUrl) }" },
        { label: "${ ui.encodeJavaScript(ui.message("htmlformentryui.viewHtmlForm.breadcrumb", ui.message(ui.format(htmlForm.form)))) }" }
    ];
</script>

<style type="text/css">
    #form-actions {
        float: right;
    }
</style>

<% if (encounter.voided) { %>
    <div id="form-deleted-warning">
        ${ ui.message("htmlformentryui.thisFormIsDeleted") }
    </div>
<% } %>

<% if (showPatientHeader) { %>
    ${ ui.includeFragment("coreapps", "patientHeader", [ patient: patient ]) }
<% } %>

<span id="form-actions" class="no-print">

<% if (customPrintPageName == null || customPrintPageName.isEmpty()) { %>
        <a class="button" id="print-button" href="javascript:window.print()">
<% } else { %>
        <a class="button" id="print-button" href="${ui.pageLink(customPrintProvider, customPrintPageName, [encounterUuid: encounter.uuid, contentDisposition: 'inline'])}" target="${customPrintTarget}">
<% } %>

        <i class="icon-print"></i>
        ${ ui.message("uicommons.print") }
    </a>
    <a class="button" id="edit-button" href="${ ui.pageLink("htmlformentryui", "htmlform/editHtmlFormWith" + editStyle + "Ui", [
            encounterId: encounter.uuid,
            patientId: patient.patient.uuid,
            returnUrl: returnUrl
    ]) }">
        <i class="icon-pencil"></i>
        ${ ui.message("uicommons.edit") }
    </a>
    <a class="button" id="delete-button">
        <i class="icon-remove"></i>
        ${ ui.message("uicommons.delete") }
    </a>
    <div style="display:none" id="confirm-delete-dialog" class="dialog">
        <div class="dialog-header">
            ${ ui.message("htmlformentryui.confirmDeleteFormHeading", ui.format(htmlForm)) }
        </div>
        <div class="dialog-content">
            <p>
                ${ ui.message("htmlformentryui.confirmDeleteForm")}
            </p>
            <br/>
            <div class="buttons">
                <button class="confirm right">Delete</button>
                <button class="cancel">Cancel</button>
            </div>
        </div>
    </div>
</span>

${ ui.includeFragment("htmlformentryui", "htmlform/viewEncounterWithHtmlForm", [
        encounter: encounter,
        htmlFormId: htmlForm
]) }
