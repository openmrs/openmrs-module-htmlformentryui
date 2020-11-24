$(function() {
    if (jq("#encounterDate span").length && jq("#encounterDate span").next().length) {
        var date= jq("#encounterDate span").first().text()
        var time = jq("#encounterDate span").next().text()
        var tz = jq("#encounterDate span").next().attr('tz');
        var dateTime = date + "T" + time+":00" +tz;
        var convertedDate = new Date(moment(dateTime,"DD/MM/YYYYTHH:mm:ssZ"));
        convertedDate = moment(convertedDate).format('DD/MM/YYYY HH:mm');
        jq("#encounterDate span").first().hide();
        jq("#encounterDate span").next().text(convertedDate);
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