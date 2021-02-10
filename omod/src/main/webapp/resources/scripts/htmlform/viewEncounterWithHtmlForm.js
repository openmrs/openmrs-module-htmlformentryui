$(function() {

    //Convert encounterDate from rfc3339 (UTC) date to client timezone
    if (jq("#encounterDate").find(".rfc3339-date").length) {
        console.log(moment('es'))
        var dateTime= jq("#encounterDate").find(".rfc3339-date").text()
        var convertUtcToClientTZ = new Date(dateTime)
        jq("#encounterDate").find(".rfc3339-date").text( formatDatetime(new Date(convertUtcToClientTZ), window.viewHtmlForm.formatDatetime, window.locale));
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