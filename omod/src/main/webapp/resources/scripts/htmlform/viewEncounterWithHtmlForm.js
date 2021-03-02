$(function() {

    //Convert encounterDate from rfc3339 (UTC) date to client timezone
    if (jq("#encounterDate").find(".rfc3339-date").length) {
        var dateTime= jq("#encounterDate").find(".rfc3339-date").text()
        var convertUtcToClientTZ = new Date(dateTime)
        jq("#encounterDate").find(".rfc3339-date").text( formatDatetimeClient(new Date(convertUtcToClientTZ), window.viewHtmlForm.datetimeFormat, window.viewHtmlForm.locale));
    }

    function formatDatetimeClient(date, format, locale) {
        var defaultFormat =  window.viewHtmlForm.dateFormat;
        try{
            moment.locale(locale);
            return moment(date).format(format);
        } catch(err) {
            return moment(date).format(defaultFormat);
        }
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