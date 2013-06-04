<%
    ui.decorateWith("appui", "standardEmrPage")
    ui.includeJavascript("uicommons", "navigator/validators.js", Integer.MAX_VALUE - 19)
    ui.includeJavascript("uicommons", "navigator/navigator.js", Integer.MAX_VALUE - 20)
    ui.includeJavascript("uicommons", "navigator/navigatorHandlers.js", Integer.MAX_VALUE - 21)
    ui.includeJavascript("uicommons", "navigator/navigatorModels.js", Integer.MAX_VALUE - 21)
    ui.includeJavascript("uicommons", "navigator/exitHandlers.js", Integer.MAX_VALUE - 22);

    def createNewVisit = createVisit ?: false

    def breadcrumbMiddle = breadcrumbOverride ?: """
        [ { label: "${ ui.escapeJs(ui.format(patient.familyName)) }, ${ ui.escapeJs(ui.format(patient.givenName)) }" , link: '${ui.pageLink("coreapps", "patientdashboard/patientDashboard", [patientId: patient.id])}'} ]
    """
%>

${ ui.includeFragment("uicommons", "validationMessages")}

${ ui.includeFragment("coreapps", "patientdashboard/patientHeader", [ patient: patient ]) }

<script type="text/javascript">
    var breadcrumbs = _.flatten([
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        ${ breadcrumbMiddle } ,
        { label: "${ ui.escapeJs(ui.format(htmlForm.form)) }" }
    ]);

    jQuery(function() {
        jq('input.submitButton').hide();
        jq('form#htmlform').append(jq('#confirmation-template').html());
        KeyboardController(jq('#htmlform').first());

        jq('input.confirm').click(function(){

            if (!jq(this).attr("disabled")) {
                jq(this).closest("form").submit();
            }

            jq(this).attr('disabled', 'disabled');
            jq(this).addClass("disabled");

        });
    });
</script>

${ ui.includeFragment("htmlformentryui", "htmlform/enterHtmlForm", [
        patient: patient,
        htmlForm: htmlForm,
        visit: visit,
        createVisit: createNewVisit,
        returnUrl: returnUrl,
        automaticValidation: false,
        cssClass: "simple-form-ui"
]) }

<script type="text/template" id="confirmation-template">
    <div id="confirmation">
        <span class="title">${ ui.message("emr.simpleFormUi.confirm.title") }</span>

        <div id="confirmationQuestion" class="container half-width">
            <h3>${ ui.message("emr.simpleFormUi.confirm.question") }</h3>

            <div class="before-dataCanvas"></div>
            <div id="dataCanvas"></div>
            <div class="after-data-canvas"></div>

            <p style="display: inline">
                <input type="submit" value="${ ui.message("emr.save") }" class="confirm right"/>
            </p>
            <p style="display: inline">
                <input type="button" value="${ ui.message("emr.no") }" class="cancel" />
            </p>
            <p>
                <span class="error field-error">${ ui.message("emr.simpleFormUi.error.emptyForm") }</span>
            </p>
        </div>
    </div>
</script>