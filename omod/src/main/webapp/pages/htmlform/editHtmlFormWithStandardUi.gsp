<%
    ui.decorateWith("appui", "standardEmrPage")

    def breadcrumbMiddle = breadcrumbOverride ?: """
        [ { label: '${ returnLabel }' , link: '${ returnUrl }'} ]
    """
%>

<script type="text/javascript">
    var breadcrumbs = _.flatten([
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        ${ breadcrumbMiddle },
        { label: "${ ui.escapeJs(ui.message("emr.editHtmlForm.breadcrumb", ui.format(htmlForm.form))) }" }
    ]);

    jq(function() {
        jq('.cancel').click(htmlForm.cancel);
    });
</script>

${ ui.includeFragment("coreapps", "patientHeader", [ patient: patient ]) }

${ ui.includeFragment("htmlformentryui", "htmlform/enterHtmlForm", [
        visit: encounter.visit,
        encounter: encounter,
        patient: patient,
        returnUrl: returnUrl
]) }