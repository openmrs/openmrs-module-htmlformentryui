var htmlForm = (function(jq) {

    // individual forms can define their own functions to execute before a form validation or submission by adding them to these lists
    // if any function returns false, no further functions are called and the validation or submission is cancelled
    var beforeValidation = new Array();     // a list of functions that will be executed before the validation of a form
    var beforeSubmit = new Array(); 		// a list of functions that will be executed before the submission of a form
    var propertyAccessorInfo = new Array();

    var tryingToSubmit = false;

    var returnUrl = '';

    disableSubmitButton = function() {
        jq('.submitButton').attr('disabled', 'true');
        jq('.submitButton').addClass("disabled");
    }

    enableSubmitButton = function() {
        jq('.submitButton').removeAttr('disabled');
        jq('.submitButton').removeClass("disabled");
    }

    findAndHighlightErrors = function() {
        /* see if there are error fields */
        var containError = false
        var ary = jq(".autoCompleteHidden");
        jq.each(ary, function(index, value){
            if(value.value == "ERROR"){
                if(!containError){

                    // TODO: get this localized?  are we even using this?
                    alert("Autocomplete answer not valid");
                    // alert("${ ui.message("htmlformentry.error.autoCompleteAnswerNotValid") }");
                    var id = value.id;
                    id = id.substring(0,id.length-4);
                    jq("#"+id).focus();
                }
                containError=true;
            }
        });
        return containError;
    }

    /*
     It seems the logic of  showAuthenticateDialog and
     findAndHighlightErrors should be in the same callback function.
     i.e. only authenticated user can see the error msg of
     */
    checkIfLoggedInAndErrorsCallback = function(isLoggedIn) {

        var state_beforeValidation=true;

        if (!isLoggedIn) {
            showAuthenticateDialog();
        }else{

            // first call any beforeValidation functions that may have been defined by the html form
            if (beforeValidation.length > 0){
                for (var i=0, l = beforeValidation.length; i < l; i++){
                    if (state_beforeValidation){
                        var fncn=beforeValidation[i];
                        state_beforeValidation=eval(fncn);
                    }
                    else{
                        // forces the end of the loop
                        i=l;
                    }
                }
            }

            // only do the validation if all the beforeValidation functions returned "true"
            if (state_beforeValidation){
                var anyErrors = findAndHighlightErrors();

                if (anyErrors) {
                    tryingToSubmit = false;
                    return;
                }else{
                    doSubmitHtmlForm();
                }
            }
        }
    }

    showAuthenticateDialog = function() {
        jq('#passwordPopup').show();
        tryingToSubmit = false;
    }

    doSubmitHtmlForm = function() {

        // first call any beforeSubmit functions that may have been defined by the form
        var state_beforeSubmit=true;
        if (beforeSubmit.length > 0){
            for (var i=0, l = beforeSubmit.length; i < l; i++){
                if (state_beforeSubmit){
                    var fncn=beforeSubmit[i];
                    state_beforeSubmit=fncn();
                }
                else{
                    // forces the end of the loop
                    i=l;
                }
            }
        }

        // only do the submit if all the beforeSubmit functions returned "true"
        if (state_beforeSubmit){
            var form = jq('#htmlform');
            // TODO hide all errors
            disableSubmitButton();
            //ui.openLoadingDialog('Submitting Form');
            jq.post(form.attr('action'), form.serialize(), function(result) {
                if (result.success) {
                    if(returnUrl) {
                        location.href = returnUrl;
                    }
                    else {
                        if (typeof(parent) !== 'undefined') {
                            parent.location.reload();
                        } else {
                            location.reload();
                        }
                     }
                } else {
                    //ui.closeLoadingDialog();
                    enableSubmitButton();
                    for (key in result.errors) {
                        showError(key, result.errors[key]);
                    }
                    // scroll to one of the errors
                    // TODO there must be a more efficient way to do this!
                    for (key in result.errors) {
                        jq(document).scrollTop(jq('#' + key).offset().top - 100);
                        break;
                    }

                    //ui.enableConfirmBeforeNavigating();
                }
            }, 'json')
                    .error(function(jqXHR, textStatus, errorThrown) {
                        //ui.closeLoadingDialog();
                        //ui.enableConfirmBeforeNavigating();

                        emr.errorAlert('Unexpected error, please contact your System Administrator: ' + textStatus);
                    });
            }
            tryingToSubmit = false;
        }

        return {

            submitHtmlForm: function()  {
                if (!tryingToSubmit) {
                    tryingToSubmit = true;
                    jq.getJSON(emr.fragmentActionLink('htmlformentryui', 'htmlform/enterHtmlForm', 'checkIfLoggedIn'), function(result) {
                        checkIfLoggedInAndErrorsCallback(result.isLoggedIn);
                    });
                }
            },

            loginThenSubmitHtmlForm: function() {
                jq('#passwordPopup').hide();
                var username = jq('#passwordPopupUsername').val();
                var password = jq('#passwordPopupPassword').val();
                jq('#passwordPopupUsername').val('');
                jq('#passwordPopupPassword').val('');
                jq.getJSON(emr.fragmentActionLink('htmlformentryui', 'htmlform/enterHtmlForm', 'checkIfLoggedIn', { user: username, pass: password }), submitHtmlForm);
            },

            getValueIfLegal: function(idAndProperty) {
                var jqField = getField(idAndProperty);
                if (jqField && jqField.hasClass('illegalValue')) {
                    return null;
                }
                return getValue(idAndProperty);
            },

            getPropertyAccessorInfo: function() {
                return propertyAccessorInfo;
            },

            getBeforeSubmit: function() {
                return beforeSubmit;
            },

            getBeforeValidation: function() {
                return beforeValidation;
            },

            setReturnUrl: function(url) {
                returnUrl = url;
            },

            // TODO: these methods (getEncounter*Date*) will have to be modified when/if we switch datepickers
            // TODO: could/should be generalized so as not to be datepicker dependent?
            // TODO: note that for these methods to work, the id of the encounterDate field must be explicitly set to "encounterDate" until HTML-482 is implemented

            setEncounterStartDateRange: function(date) {
                if (getField('encounterDate.value')) {
                    getField('encounterDate.value').datepicker('option', 'minDate', date);
                }
            },

            setEncounterStopDateRange: function(date) {
                if (getField('encounterDate.value')) {
                    getField('encounterDate.value').datepicker('option', 'maxDate', date);
                }
            },

            setEncounterDate: function(date) {
                if (getField('encounterDate.value')) {
                    getField('encounterDate.value').datepicker('setDate', date);
                }
            },

            disableEncounterDateManualEntry: function() {
                if (getField('encounterDate.value')) {
                    getField('encounterDate.value').attr( 'readOnly' , 'true' );
                }
            },

            showDiv: function(id) {
                var div = document.getElementById(id);
                if ( div ) { div.style.display = ""; }
            },

            hideDiv: function(id) {
                var div = document.getElementById(id);
                if ( div ) { div.style.display = "none"; }
            }


        }

    })(jQuery);