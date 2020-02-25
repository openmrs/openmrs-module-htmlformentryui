//Uses the namespace pattern from http://stackoverflow.com/a/5947280
// expects to extend htmlForm defined in the core HFE module
(function( htmlForm, jq, undefined) {

    // individual forms can define their own functions to execute before a form validation or submission by adding them to these lists
    // if any function returns false, no further functions are called and the validation or submission is cancelled
    var beforeValidation = new Array();     // a list of functions that will be executed before the validation of a form
    var beforeSubmit = new Array(); 		// a list of functions that will be executed before the submission of a form
    var propertyAccessorInfo = new Array();

    var whenObsHasValueThenDisplaySection = { };

    var tryingToSubmit = false;

    var returnUrl = '';

    var successFunction = function(result) {
        if (result.goToUrl) {
            emr.navigateTo({ applicationUrl: result.goToUrl });
        } else {
            goToReturnUrl(result.encounterId);
        }
    }

    var disableSubmitButton = function() {
        jq('.submitButton.confirm').attr('disabled', 'disabled');
        jq('.submitButton.confirm').addClass("disabled");
        if (tryingToSubmit) {
            jq('.submitButton.confirm .icon-spin').css('display', 'inline-block');
        }
    }

    var enableSubmitButton = function() {
        jq('.submitButton.confirm').removeAttr('disabled', 'disabled');
        jq('.submitButton.confirm').removeClass("disabled");
        jq('.submitButton.confirm .icon-spin').css('display', 'none');
    }

    var submitButtonIsDisabled = function() {
        return jq(".submitButton.confirm").is(":disabled");
    }

    var findAndHighlightErrors = function() {
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
    var checkIfLoggedInAndErrorsCallback = function(isLoggedIn) {

        var state_beforeValidation=true;

        if (!isLoggedIn) {
            showAuthenticateDialog();
        }else{

            // first call any beforeValidation functions that may have been defined by the html form
            if (beforeValidation.length > 0){
                for (var i=0, l = beforeValidation.length; i < l; i++){
                    if (state_beforeValidation){
                        var fncn=beforeValidation[i];
                        state_beforeValidation=fncn.call(htmlForm);
                    }
                    else{
                        // forces the end of the loop
                        i=l;
                    }
                }
            }

            // only do the validation if all the beforeValidation functions returned "true"
            if (state_beforeValidation) {
                var anyErrors = findAndHighlightErrors();

                if (anyErrors) {
                    tryingToSubmit = false;
                    return;
                } else {
                    doSubmitHtmlForm();
                }
            }
            else {
                tryingToSubmit = false;
            }
        }
    }

    var showAuthenticateDialog = function() {
        jq('#passwordPopup').show();
        tryingToSubmit = false;
    }

    // if an encounter id is passed in, then insert it at the beginning of the query string
    var goToReturnUrl = function(encounterId) {
        if (returnUrl) {
            if (encounterId) {
              var encounterParameter =  "encounterId=" + encounterId;
              var index = returnUrl.indexOf('?');
              if (index == -1) {
                  returnUrl = returnUrl + '?' + encounterParameter;
              } else {
                  returnUrl = returnUrl.substring(0,index+1) + encounterParameter + '&' + returnUrl.substring(index+1);
              }
            }
            location.href = returnUrl;
        }
        else {
            if (typeof(parent) !== 'undefined') {
                parent.location.reload();
            } else {
                location.reload();
            }
        }
    }

    var doSubmitHtmlForm = function() {

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
        // also, hack to double check to  disallow form submittal if submit button is disabled (prevent multiple submits)
        if (state_beforeSubmit && !submitButtonIsDisabled()){
            disableSubmitButton();
            var form = jq('#htmlform');
            var formData = false;
            // Check whether FormData is supported
            if (window.FormData) {
                formData = new FormData(form[0]);
            }
			jq(".error", form).text(""); //clear errors
            //ui.openLoadingDialog('Submitting Form');

            jq.ajax({
                type: 'POST',
                url: form.attr('action'),
                data: formData ? formData : form.serialize(),
                cache: false,
                contentType: false,
                processData: false,
                dataType: 'json',
                success: function (result) {
                    if (result.success) {
                        tryingToSubmit = false;
                        successFunction(result);
                    }
                    else {
                        enableSubmitButton();
                        tryingToSubmit = false;
                        for (key in result.errors) {
                            showError(key, result.errors[key]);
                        }
                        // scroll to one of the errors
                        // TODO there must be a more efficient way to do this!
                        for (key in result.errors) {
                            jq(document).scrollTop(jq('#' + key).offset().top - 100);
                            break;
                        }

                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    emr.errorAlert('Unexpected error, please contact your System Administrator: ' + textStatus);
                }
            });
        }
        else {
            tryingToSubmit = false;
        }
    };

    htmlForm.submitHtmlForm = function()  {
        if (!tryingToSubmit) {    // don't allow form submittal if submit button is disabled (disallows multiple submits)
            tryingToSubmit = true;
            jq.getJSON(emr.fragmentActionLink('htmlformentryui', 'htmlform/enterHtmlForm', 'checkIfLoggedIn'), function(result) {
                checkIfLoggedInAndErrorsCallback(result.isLoggedIn);
            });
        }
    };

    htmlForm.loginThenSubmitHtmlForm = function() {
        jq('#passwordPopup').hide();
        var username = jq('#passwordPopupUsername').val();
        var password = jq('#passwordPopupPassword').val();
        jq('#passwordPopupUsername').val('');
        jq('#passwordPopupPassword').val('');
        jq.getJSON(emr.fragmentActionLink('htmlformentryui', 'htmlform/enterHtmlForm', 'authenticate', { user: username, pass: password }), submitHtmlForm);
    };

    htmlForm.cancel = function() {
        goToReturnUrl();
    };

    htmlForm.getValueIfLegal = function(idAndProperty) {
        var jqField = getField(idAndProperty);
        if (jqField && jqField.hasClass('illegalValue')) {
            return null;
        }
        return getValue(idAndProperty);
    };

    htmlForm.getPropertyAccessorInfo = function() {
        return propertyAccessorInfo;
    };

    htmlForm.getBeforeSubmit = function() {
        return beforeSubmit;
    };

    htmlForm.getBeforeValidation = function() {
        return beforeValidation;
    };

    htmlForm.setReturnUrl = function(url) {
        returnUrl = url;
    };

    htmlForm.getReturnUrl = function() {
        return returnUrl;
    };

    htmlForm.setSuccessFunction = function(fn) {
        successFunction = fn;
    };

    // TODO: these methods (getEncounter*Date*) will have to be modified when/if we switch datepickers
    // TODO: could/should be generalized so as not to be datepicker dependent?

    htmlForm.setEncounterStartDateRange = function(date) {
        if (getField('encounterDate.value')) {
            getField('encounterDate.value').datepicker('option', 'minDate', date);
        }
    };

    htmlForm.setEncounterStopDateRange = function(date) {
        if (getField('encounterDate.value')) {
            getField('encounterDate.value').datepicker('option', 'maxDate', date);
        }
    };

    htmlForm.setEncounterDate = function(date) {
        if (getField('encounterDate.value')) {
            getField('encounterDate.value').datepicker('setDate', date);
        }
    };

    htmlForm.disableEncounterDateManualEntry = function() {
        if (getField('encounterDate.value')) {
            getField('encounterDate.value').attr( 'readOnly' , 'true' );
        }
    };

    htmlForm.showDiv = function(id) {
        var div = document.getElementById(id);
        if ( div ) { div.style.display = ""; }
    };

    htmlForm.hideDiv = function(id) {
        var div = document.getElementById(id);
        if ( div ) { div.style.display = "none"; }
    }

    htmlForm.disableSubmitButton = function() {
        disableSubmitButton();
    }

    htmlForm.enableSubmitButton = function() {
        // don't allow the submit button to be enabled if trying to submit
        if (!tryingToSubmit) {
            enableSubmitButton();
        }
    }


}( window.htmlForm = window.htmlForm || {}, jQuery ));
