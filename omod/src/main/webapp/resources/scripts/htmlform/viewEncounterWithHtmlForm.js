$(function() {

    if (jq(".dateTimeWithTimezone").length) {
        var dateTime= jq(".dateTimeWithTimezone").text()
        var convertUtcToClientTZ = new Date(dateTime)
        jq(".dateTimeWithTimezone").text(moment(convertUtcToClientTZ).format('DD/MM/YYYY HH:mm:ss'));
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