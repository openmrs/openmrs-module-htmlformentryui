//Uses the namespace pattern from http://stackoverflow.com/a/5947280
// expects to extend htmlForm defined in the core HFE module
(function( htmlForm, jq, undefined) {

    // individual forms can define their own functions to execute before a form validation or submission by adding them to these lists
    // if any function returns false, no further functions are called and the validation or submission is cancelled
    var beforeValidation = new Array();     // a list of functions that will be executed before the validation of a form
    var beforeSubmit = new Array(); 		// a list of functions that will be executed before the submission of a form

    // individual forms can define their own functions to execute on the case of a failed form submission
    var afterSubmitFailure = new Array();

    var propertyAccessorInfo = new Array();
    var binaryDataInputs = [];

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

    var showModal = function() {
        jq('#modal-overlay').fadeIn();
    }

    var hideModal = function() {
        jq('#modal-overlay').fadeOut();
    }



    /**
     * Change the hours, minutes and seconds dropdowns to new date with client timezone
     * also change the encounterDate datepicker current date to new date with client timezone
     **/
    var changeTimeWidgetClientTimeZone = function (clientDateTime) {
        getField('encounterDate.value').datepicker('setDate', clientDateTime)
        jq("#encounterDate").find(".hfe-hours").val(clientDateTime.getHours()).change();
        jq("#encounterDate").find(".hfe-minutes").val(clientDateTime.getMinutes()).change();
        jq("#encounterDate").find(".hfe-seconds").val(clientDateTime.getSeconds()).change();
    }

    /**
     * Receives a datetime on ISO8601 format and return only the date part.
     * Example: receives the datetime 2021-04-01T15:00:00.000+0000, the output is 04-01-2021.
     **/
    var extractDate = function (date) {
        return moment(date.split('T')[0]).toDate()
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
     Only an authenticated user gets as far as validation. If the session has expired, the user
     is sent to the home page to log in again (there is no in-page re-login).
     */
    var checkIfLoggedInAndErrorsCallback = function(isLoggedIn) {

        var state_beforeValidation=true;

        if (isLoggedIn === null) {
            // couldn't reach the server; keep the user on the form so nothing is lost
            tryingToSubmit = false;
            emr.errorAlert(sessionExpiry.messages.serverUnreachable);
        } else if (!isLoggedIn) {
            goToHomePage();
        } else {

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

    /*
     * Session expiry warning
     *
     * The server session is only kept alive by requests to the server (typing in the form doesn't
     * count), so we track the time since the last request: page load, plus any jQuery ajax call.
     * Shortly before the session times out we show a countdown with a link that refreshes the
     * session. If it does time out, we send the user to the home page to log in again.
     */
    var SESSION_CHECK_GRACE_MS = 2000;      // check a little after the expected expiry, so the server has expired it
    var SESSION_RETRY_MS = 10000;           // when the server can't be reached, try again after this long

    var sessionExpiry = {
        timeoutMs: 0,
        warningMs: 0,
        homeUrl: null,
        expiresAt: 0,
        timer: null,
        checking: false,
        redirecting: false,
        messages: {
            serverUnreachable: 'Unable to reach the server. Please check your connection and try again.'
        }
    };

    var goToHomePage = function() {
        if (sessionExpiry.redirecting) {
            return;
        }
        sessionExpiry.redirecting = true;
        if (sessionExpiry.timer) {
            clearInterval(sessionExpiry.timer);
            sessionExpiry.timer = null;
        }
        location.href = sessionExpiry.homeUrl || '/';
    };

    /*
     * Asks the server whether the user is still logged in, which also keeps a live session alive.
     * Calls back with true, false, or null if the server couldn't be reached.
     * Once the session has gone, the authentication filter redirects this request to the login page,
     * which comes back as a JSON string (or as HTML), so anything other than an explicit
     * isLoggedIn === true means logged out.
     */
    var checkSession = function(callback) {
        jq.ajax({
            url: emr.fragmentActionLink('htmlformentryui', 'htmlform/enterHtmlForm', 'checkIfLoggedIn'),
            dataType: 'json',
            cache: false,
            global: false   // don't let our own check count as activity in the ajaxComplete handler
        }).done(function(result) {
            callback(!!result && result.isLoggedIn === true);
        }).fail(function(jqXHR) {
            var serverUnreachable = jqXHR.status === 0 || jqXHR.status >= 500;
            callback(serverUnreachable ? null : false);
        });
    };

    // formats a number of seconds as MM:SS, e.g. 90 -> "01:30"
    var formatCountdown = function(totalSeconds) {
        var minutes = Math.floor(totalSeconds / 60);
        var seconds = totalSeconds % 60;
        return (minutes < 10 ? '0' : '') + minutes + ':' + (seconds < 10 ? '0' : '') + seconds;
    };

    var showSessionExpiryDialog = function(secondsLeft) {
        jq('#session-expiry-countdown').text(formatCountdown(secondsLeft));
        var dialog = jq('#session-expiry-overlay');
        if (!dialog.is(':visible')) {
            jq('#session-expiry-error').hide();
            dialog.show();
            jq('#session-expiry-refresh').focus();
        }
    };

    var hideSessionExpiryDialog = function() {
        jq('#session-expiry-overlay').hide();
    };

    var restartSessionExpiryCountdown = function() {
        if (sessionExpiry.redirecting) {
            return;
        }
        sessionExpiry.expiresAt = Date.now() + sessionExpiry.timeoutMs;
        hideSessionExpiryDialog();
    };

    var onSessionExpiryTick = function() {
        if (sessionExpiry.checking || sessionExpiry.redirecting) {
            return;
        }
        var msLeft = sessionExpiry.expiresAt - Date.now();
        if (msLeft <= -SESSION_CHECK_GRACE_MS) {
            // we expect the session to have expired by now; confirm with the server before leaving,
            // since another tab using the same session may have kept it alive
            sessionExpiry.checking = true;
            checkSession(function(isLoggedIn) {
                sessionExpiry.checking = false;
                if (isLoggedIn === true) {
                    restartSessionExpiryCountdown();
                } else if (isLoggedIn === false) {
                    goToHomePage();
                } else {
                    sessionExpiry.expiresAt = Date.now() + SESSION_RETRY_MS - SESSION_CHECK_GRACE_MS;
                }
            });
        } else if (msLeft <= sessionExpiry.warningMs) {
            showSessionExpiryDialog(Math.max(0, Math.ceil(msLeft / 1000)));
        }
    };

    var refreshSession = function() {
        if (sessionExpiry.checking || sessionExpiry.redirecting) {
            return;
        }
        sessionExpiry.checking = true;
        checkSession(function(isLoggedIn) {
            sessionExpiry.checking = false;
            if (isLoggedIn === true) {
                restartSessionExpiryCountdown();
            } else if (isLoggedIn === false) {
                goToHomePage();
            } else {
                jq('#session-expiry-error').text(sessionExpiry.messages.serverUnreachable).show();
            }
        });
    };

    /*
     * Starts tracking the session. Options:
     *   timeoutSeconds: the server session timeout; 0 or less means don't track the session
     *   warningSeconds: how long before expiry to show the countdown; 0 or less means no countdown
     *   homeUrl: where to send the user once the session has expired
     *   messages: localized messages (serverUnreachable)
     */
    htmlForm.initSessionExpiryWarning = function(options) {
        var timeoutSeconds = parseInt(options.timeoutSeconds, 10) || 0;
        var warningSeconds = parseInt(options.warningSeconds, 10) || 0;

        if (sessionExpiry.timer) {
            clearInterval(sessionExpiry.timer);
            sessionExpiry.timer = null;
        }
        jq(document).off('.sessionExpiry');
        jq('#session-expiry-refresh').off('.sessionExpiry');

        sessionExpiry.homeUrl = options.homeUrl;
        jq.extend(sessionExpiry.messages, options.messages);
        if (timeoutSeconds <= 0) {
            return;
        }
        sessionExpiry.timeoutMs = timeoutSeconds * 1000;
        sessionExpiry.warningMs = Math.min(Math.max(warningSeconds, 0), timeoutSeconds) * 1000;
        restartSessionExpiryCountdown();

        // any successful request to the server keeps the session alive
        jq(document).on('ajaxComplete.sessionExpiry', function(event, jqXHR) {
            if (jqXHR && jqXHR.status > 0) {
                restartSessionExpiryCountdown();
            }
        });

        // browsers slow down timers in background tabs, so re-check as soon as the tab is visible again
        jq(document).on('visibilitychange.sessionExpiry', function() {
            if (!document.hidden) {
                onSessionExpiryTick();
            }
        });

        jq('#session-expiry-refresh').on('click.sessionExpiry', function(e) {
            e.preventDefault();
            refreshSession();
        });

        sessionExpiry.timer = setInterval(onSessionExpiryTick, 1000);
    };

    var goToReturnUrl = function(encounterId) {
        if (returnUrl) {
            // special-case where we want to post a message to the parent window instead of navigating (used for O3 integration)
            if (returnUrl.startsWith("post-message:")) {
                const message = returnUrl.split(":")[1];
                window.parent.postMessage(message, "*");
            }
            // main use case--redirect to a URL
            else {
                // if an encounter id is passed in, then insert it at the beginning of the query string
                if (encounterId) {
                    var encounterParameter = "encounterId=" + encounterId;
                    var index = returnUrl.indexOf('?');
                    if (index == -1) {
                        returnUrl = returnUrl + '?' + encounterParameter;
                    } else {
                        returnUrl = returnUrl.substring(0, index + 1) + encounterParameter + '&' + returnUrl.substring(index + 1);
                    }
                }
                location.href = returnUrl;
            }
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
            showModal();
            var form = jq('#htmlform');
            var formData = new FormData(form[0]);
            binaryDataInputs.forEach(e => {
                formData.set(e.formFieldName, e.binaryData, e.fileName);
            })

            // the "returnUrl" hidden input only reflects the value at page render time; if a form
            // has since called htmlForm.setReturnUrl(...) (e.g. for O3 "post-message:" integration),
            // that live value must win over whatever is still sitting in the hidden input
            if (returnUrl) {
                formData.set('returnUrl', returnUrl);
            } else {
                formData.delete('returnUrl');
            }

			jq(".error").text(""); //clear errors

            jq.ajax({
                type: 'POST',
                url: form.attr('action'),
                data: formData,
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
                      // call any afterSubmitFailure functions that may have been defined by the form
                      if (afterSubmitFailure.length > 0){
                        for (var i=0, l = afterSubmitFailure.length; i < l; i++){
                          afterSubmitFailure[i]();
                        }
                      }
                      enableSubmitButton();
                      hideModal();
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
            checkSession(checkIfLoggedInAndErrorsCallback);
        }
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

    htmlForm.addBinaryData = function(formFieldName, binaryData, fileName) {
        binaryDataInputs.push({formFieldName, binaryData, fileName});
    };

    htmlForm.getBeforeSubmit = function() {
        return beforeSubmit;
    };

    htmlForm.getBeforeValidation = function() {
        return beforeValidation;
    };

    htmlForm.getAfterSubmitFailure = function() {
        return afterSubmitFailure;
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

    //Set encounterDate datepicker start date
    htmlForm.setEncounterStartDateRange = function(date , handletimezones) {
        if (getField('encounterDate.value')) {
            if (handletimezones) {
                var startDateWithClientTimezone = new Date(date)
                getField('encounterDate.value').datepicker('option', 'minDate', startDateWithClientTimezone)
            }else{
                getField('encounterDate.value').datepicker('option', 'minDate',  extractDate(date));
            }
        }
    };

    //Set encounterDate datepicker stop date
    htmlForm.setEncounterStopDateRange = function(date, handletimezones) {
        //Set browser timezone
        if(jq("#encounterDate").find(".hfe-timezone")){
            jq("#encounterDate").find(".hfe-timezone").val(Intl.DateTimeFormat().resolvedOptions().timeZone)
        }
        if (getField('encounterDate.value')) {
            if (handletimezones) {
                var stopDateWithClientTimezone = new Date(date)
                getField('encounterDate.value').datepicker('option', 'maxDate', stopDateWithClientTimezone > new Date ? new Date : stopDateWithClientTimezone)
            }else{
                getField('encounterDate.value').datepicker('option', 'maxDate', extractDate(date));
            }
        }
    };

    //Set the encounterDate datepicker value, this value will be overwritten if using timezones.
    htmlForm.setEncounterDate = function(date) {
        if (getField('encounterDate.value')) {
            getField('encounterDate.value').datepicker('setDate',  extractDate(date));
        }
    };

    //Used to adjust encounterDate with client timezone
    htmlForm.adjustEncounterDatetimeWithTimezone = function(setDateTime ) {
        if (jq("#encounterDate").find(".hfe-timezone").length) {
            //Set encounterDate default date and time
            var dateWithClientTimeZone =  new Date(setDateTime)
            changeTimeWidgetClientTimeZone(dateWithClientTimeZone);
        };
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

// backwards compatibility hack to make sure jQuery is available as "$j" for old forms that use it
window['$j'] = window['$j'] || jQuery;
