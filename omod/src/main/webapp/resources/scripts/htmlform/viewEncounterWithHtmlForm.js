$(function() {

    if (jq(".rfc3339-date").length) {
        var dateTime= jq(".rfc3339-date").text()
        var convertUtcToClientTZ = new Date(dateTime)
        jq(".rfc3339-date").text(moment(convertUtcToClientTZ).format('DD/MM/YYYY HH:mm:ss'));
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