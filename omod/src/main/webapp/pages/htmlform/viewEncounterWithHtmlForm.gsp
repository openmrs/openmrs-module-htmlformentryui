<%
    ui.decorateWith("appui", "standardEmrPage")

    ui.includeJavascript("htmlformentryui", "htmlform/viewEncounterWithHtmlForm.js")
    ui.includeCss("htmlformentryui", "htmlform/viewEncounterWithHtmlForm.css")
%>

<script type="text/javascript">
    window.viewHtmlForm = {
        encounterId: ${ encounter.id },
        returnUrl: "${ ui.escapeJs(returnUrl) }",
        htmlFormId: ${htmlForm.id}
    };

    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        <% if (visit) { %>
            { label: "${ ui.escapeJs(ui.format()) }", link: '' },
        <% } %>
        { label: "${ ui.escapeJs(ui.message("htmlformentryui.viewHtmlForm.breadcrumb", ui.format(htmlForm.form))) }" }
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

<span id="form-actions">
    <a class="button" href="javascript:window.print()">
        <i class="icon-print"></i>
        ${ ui.message("uicommons.print") }
    </a>
    <a class="button" href="${ ui.pageLink("htmlformentryui", "htmlform/editHtmlFormWithStandardUi", [
            encounterId: encounter.uuid,
            patientId: patient.patient.uuid
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