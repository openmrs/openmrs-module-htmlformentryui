$(function() {

    if (jq(".hfe-dateTime").length) {
        jq("#encounterDate ").find(".value").each(function() {
            jq(this).hide();
        })
        let  clientDateWithTimezone = new Date(jq(".hfe-dateTime").attr("value"));
        jq(".hfe-dateTime").text(moment(clientDateWithTimezone).format("DD/MM/YYYY HH:mm:ss"))
    }

    var dialog = emr.setupConfirmationDialog({
        selector: '#confirm-delete-dialog',
        actions: {
            confirm: function() {
                $.post('/' + OPENMRS_CONTEXT_PATH + '/module/htmlformentry/deleteEncounter.form', window.viewHtmlForm,
                function() {
                    location.href = window.viewHtmlForm.returnUrl;
                });
                emr.successMessage("Form data has been succesfully deleted");
                console.log("yes");              
            },
            cancel: function() {
                console.log("no");
            }
        }
    });

    $('#delete-button').click(function() {
        dialog.show();
    });
});