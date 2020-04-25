//Uses the namespace pattern from http://stackoverflow.com/a/5947280
// expects to extend htmlForm defined in the core HFE module
(function( flowsheet, jq, undefined) {

    var patientId = null;
    var headerForm = null;
    var headerEncounterId = null;
    var headerEncounterDate = null;
    var requireEncounter = true;
    var flowsheets = [];
    var viewOnly = false;
    var currentlyEditingFormName = null;
    var currentlyEditingEncounterId = null;
    var htmlformJs = null;
    var defaultLocationId = null;
    var validationErrors = {};
    var dirty = false;
    var loadingEncounters = [];
    var patientDashboardUrl = null;
    var flowsheetExtension = null; // This can be overridden by modules to provide custom functionality
    var encounterIdToEncounterTypeUuidMap = null;

    function Flowsheet(index, formName, encounterIds, encounterTypeUuid) {
        this.index = index;
        this.formName = formName;
        this.encounterIds = encounterIds;
        this.encounterTypeUuid = encounterTypeUuid;

        this.addEncounterId = function(eId) {
            if (this.encounterIds.indexOf(eId) < 0) {
                this.encounterIds.push(eId);
            }
        };

        this.removeEncounterId = function(eId) {
            var index = this.encounterIds.indexOf(eId);
            if (index >= 0) {
                this.encounterIds.splice( index, 1 );
            }
        };
    }

    flowsheet.setPatientId = function(pId) {
        patientId = pId;
    };

    flowsheet.setHeaderForm = function(formName) {
        headerForm = formName;
    };

    flowsheet.setHeaderEncounterId = function(eId) {
        headerEncounterId = eId;
    };

    flowsheet.setHeaderEncounterDate = function(encounterDate) {
        headerEncounterDate = encounterDate;
    };

    flowsheet.setRequireEncounter = function(reqEnc) {
        requireEncounter = reqEnc;
    };

    //used because we only allow editing encounters that have the same encounter type as the form
    flowsheet.setEncounterIdToEncounterTypeUuidMap = function(flowsheetEncounterTypes) {
      encounterIdToEncounterTypeUuidMap = flowsheetEncounterTypes;
    };

  flowsheet.addEncounterIdToEncounterTypeUuidMap = function(encounterId, encounterTypeUuid) {
      if (typeof encounterId !== 'undefined' && typeof encounterTypeUuid !== 'undefined') {
        encounterIdToEncounterTypeUuidMap['' + encounterId] = encounterTypeUuid;
      }
  };

    flowsheet.getEncounterTypeUuid = function(encId) {
      return encounterIdToEncounterTypeUuidMap[encId];
    };

    flowsheet.addFlowsheet = function(index, formName, encounterIds, encounterTypeUuid) {
        flowsheets.push(new Flowsheet(index, formName, encounterIds, encounterTypeUuid));
    };

    flowsheet.getFlowsheet = function(formName) {
        for (var i=0; i<flowsheets.length; i++) {
            if (flowsheets[i].formName == formName) {
                return flowsheets[i];
            }
        }
    };

    flowsheet.hasFlowsheetEncounters = function() {
        for (var i=0; i<flowsheets.length; i++) {
            if (flowsheets[i].encounterIds.length > 0) {
                return true;
            }
        }
        return false
    };

    flowsheet.removeEncounterId = function(eId) {
        for (var i=0; i<flowsheets.length; i++) {
            flowsheets[i].removeEncounterId(eId);
        }
    };

    flowsheet.getCurrentlyEditingFormName = function() {
        return currentlyEditingFormName;
    };

    flowsheet.setCurrentlyEditingFormName = function(formName) {
        currentlyEditingFormName = formName;
        flowsheet.clearDirty();
    };

    flowsheet.setCurrentlyEditingEncounterId = function(encId) {
        currentlyEditingEncounterId = encId;
        flowsheet.clearDirty();
    };

    flowsheet.setHtmlFormJs = function(htmlform) {
        htmlformJs = htmlform;
        htmlForm.setSuccessFunction(function(result) {
            flowsheet.successFunction(result);
        });
        htmlform.getBeforeSubmit().push(flowsheet.isValidForSubmission);
    };

    flowsheet.setDefaultLocationId = function(locationId) {
        defaultLocationId = locationId;
    };

  flowsheet.getDefaultLocationId = function() {
    return defaultLocationId;
  };

    flowsheet.isDirty = function() {
        return dirty;
    };

    flowsheet.setDirty = function() {
        dirty = true;
    };

    flowsheet.clearDirty = function() {
        dirty = false;
    };

    flowsheet.getFlowsheetExtension = function() {
        return flowsheetExtension;
    }

    flowsheet.setFlowsheetExtension = function(ext) {
        flowsheetExtension = ext;
    }

    flowsheet.isValidForSubmission = function() {
        return jq.isEmptyObject(validationErrors);
    };

    flowsheet.getValidationErrors = function() {
        return validationErrors;
    }

    flowsheet.focusFirstObs = function() {
        var firstObsField = jq(".focus-field :input:visible:enabled:first");
        if (firstObsField && firstObsField.length > 0) {
            firstObsField.focus();
        }
        else {
            var firstField = jq(":input:visible:enabled:first");
            if (firstField && firstField.length > 0) {
                firstField.focus();
            }
        }
    };

    flowsheet.printForm = function() {
        window.print();
    };

    flowsheet.setPatientDashboardUrl = function(url) {
        patientDashboardUrl = url;
    };

    flowsheet.backToPatientDashboard = function() {
        document.location.href=patientDashboardUrl;
    };

    flowsheet.showErrorMessage = function(msg) {
        jq("#error-message-section").show().html(msg + '<a href="#" style="padding-left:20px;" onclick="flowsheet.clearErrorMessage();">Clear<span >');
    };

    flowsheet.clearErrorMessage = function(msg) {
        jq("#error-message-section").empty().hide();
    };

    flowsheet.viewHeader = function() {
        var editMode = !requireEncounter;
        loadHtmlFormForEncounter(headerForm, headerEncounterId, null, editMode,function(data) {
            jq('#header-section').html(data);
            flowsheet.toggleViewFlowsheet();
        });
    };

    flowsheet.enableViewOnly = function() {
        viewOnly = true;
    };

    flowsheet.toggleViewFlowsheet = function() {
        jq('#header-section').show();
        jq(".form-action-link").show();
        if (viewOnly || !requireEncounter) {
            jq("#edit-header-link").hide();
        }
        jq("#delete-button").hide();
        jq("#cancel-button").hide();
        jq('#visit-edit-section').hide();
        flowsheet.setCurrentlyEditingFormName(null);
        flowsheet.setCurrentlyEditingEncounterId(null);
        flowsheet.showVisitTable();
    };

    flowsheet.enterHeader = function() {
        flowsheet.setCurrentlyEditingFormName(headerForm);
        flowsheet.setCurrentlyEditingEncounterId(headerEncounterId);
        loadHtmlFormForEncounter(headerForm, headerEncounterId, headerEncounterDate,true,function(data) {
            jq('#header-section').html(data);
            setupForm(jq('#header-section'));
            jq(".flowsheet-section").hide();
            flowsheet.focusFirstObs();
        });
    };

    flowsheet.deleteCurrentEncounter = function() {
        if (currentlyEditingFormName == headerForm) {
            if (flowsheet.hasFlowsheetEncounters()) {
                flowsheet.showErrorMessage('You cannot delete a flowsheet that has recorded visits');
            }
            else {
                deleteEncounter(headerEncounterId, function(data) {
                    if (data.success) {
                        flowsheet.backToPatientDashboard();
                    }
                    else {
                        flowsheet.showErrorMessage(data.message);
                    }
                });
            }
        }
        else {
            deleteEncounter(currentlyEditingEncounterId, function(data) {
                if (data.success) {
                    jq("#visit-table-row-" + currentlyEditingEncounterId).remove(); // Remove old row for this encounter
                    flowsheet.removeEncounterId(currentlyEditingEncounterId);
                    flowsheet.toggleViewFlowsheet();
                    for (var i=0; i<flowsheets.length; i++) {
                        var fs = flowsheets[i];
                        if (fs.length == 0) {
                            jq("#flowsheet-section-"+fs.index).children().remove();
                        }
                    }
                }
                else {
                    flowsheet.showErrorMessage(data.message);
                }
            });
        }
    };

    var deleteEncounter = function(encId, callback) {
        if (confirm('Are you sure you wish to delete this?')) {
            var deleteUrl = emr.fragmentActionLink('htmlformentryui', 'htmlform/encounterAction', 'delete', {
                "encounter": encId,
                "reason": 'Deleted on flowsheet'
            });
            jq.getJSON(deleteUrl, function (data) {
                callback(data);
            });
        }
    };

    flowsheet.enterVisit = function(formName, encounterDate) {

        flowsheet.setCurrentlyEditingFormName(formName);
        loadHtmlFormForEncounter(formName, null, encounterDate,true, function(data) {
            var fs = flowsheet.getFlowsheet(formName);
            jq('#flowsheet-edit-section-'+fs.index).html(data).show();
            setupForm(jq('#flowsheet-edit-section-'+fs.index));
            jq("#header-section").hide();
            jq(".flowsheet-section").hide();
            flowsheet.focusFirstObs();
        });
    };

    flowsheet.editVisit = function(formName, encId) {
        flowsheet.setCurrentlyEditingFormName(formName);
        flowsheet.setCurrentlyEditingEncounterId(encId);
        loadHtmlFormForEncounter(formName, encId, null,true, function(data) {
            var fs = flowsheet.getFlowsheet(formName);
            jq('#flowsheet-edit-section-'+fs.index).html(data).show();
            setupForm(jq('#flowsheet-edit-section-'+fs.index));
            jq("#header-section").hide();
            jq(".flowsheet-section").hide();
            flowsheet.focusFirstObs();
        });
    };

    flowsheet.cancelEdit = function() {
        if (currentlyEditingFormName == headerForm) {
            if (headerEncounterId == null) {
                flowsheet.backToPatientDashboard();
            }
            else {
                flowsheet.viewHeader();
            }
        }
        else {
            jq('.flowsheet-edit-section').empty();
            flowsheet.toggleViewFlowsheet();
        }
        return false;
    };

    flowsheet.successFunction = function(result) {
        if (currentlyEditingFormName == headerForm) {
            flowsheet.setHeaderEncounterId(result.encounterId);
            flowsheet.viewHeader();
        }
        else {
            jq("#visit-table-row-"+result.encounterId).remove(); // Remove old row for this encounter
            var currentFlowsheet = flowsheet.getFlowsheet(currentlyEditingFormName);
            currentFlowsheet.addEncounterId(result.encounterId);
            flowsheet.addEncounterIdToEncounterTypeUuidMap(result.encounterId, result.encounterTypeUuid);
            flowsheet.loadIntoFlowsheet(currentlyEditingFormName, result.encounterId, currentFlowsheet.encounterTypeUuid, true); // Add new row for this encounter
        }
        return false;
    };

    flowsheet.showVisitTable = function() {
        jq(".flowsheet-edit-section").hide();
        jq(".flowsheet-section").show();
        if (flowsheet.getFlowsheetExtension()) {
            flowsheetExtension.afterShowVisitTable(flowsheet);
        }
    };

    flowsheet.loadVisitTable = function() {
        jq("#header-section").show();
        if (flowsheet.getFlowsheetExtension()) {
            flowsheetExtension.beforeLoadVisitTable(flowsheet);
        }
        for (var i=0; i<flowsheets.length; i++) {
            var fs = flowsheets[i];
            for (var j=0; j<fs.encounterIds.length; j++) {
                loadingEncounters.push(fs.encounterIds[j]);
                flowsheet.loadIntoFlowsheet(fs.formName, fs.encounterIds[j], fs.encounterTypeUuid);
            }
        }
    };

    /**
     * TODO: Document this at a higher level in the module documentation
     * Loads a row into the visit table
     * This will insert the row based on the encounter date such that it is in date descending order
     * This relies upon the htmlform having the following structure:
     *  The entire form structured as a table with class visit-table
     *  The data entry form in a single row with class visit-table-row
     *  The encounter date tag in a cell with class .visit-date
     */
    flowsheet.loadIntoFlowsheet = function(formName, encId, encTypeUuid, showVisitTable) {
        loadHtmlFormForEncounter(formName, encId, null,false, function(data) {
            showVisitTable = showVisitTable || false;
            var newRow = jq(data).find(".visit-table-row");
            var newVisitMoment = flowsheet.extractVisitMoment(newRow);
            if (flowsheet.getFlowsheetExtension()) {
                flowsheetExtension.beforeLoadVisitRow(flowsheet, formName, encId, data);
            }
            var fs = flowsheet.getFlowsheet(formName);
            var section = jq("#flowsheet-section-"+fs.index);
            var table = section.find(".visit-table");
            var inserted = false;
            if (table && table.length > 0) {
                addLinksToVisitRow(newRow, formName, encId, encTypeUuid);
                var existingRows = table.find(".visit-table-row")
                jq.each(existingRows, function(index, currentRow) {
                    var currentMoment = flowsheet.extractVisitMoment(currentRow);
                    if (currentMoment.isBefore(newVisitMoment) && !inserted) {
                        newRow.insertBefore(currentRow);
                        inserted = true;
                    }
                    else {
                        if (index == (existingRows.length-1) && !inserted) {
                            newRow.insertAfter(currentRow);
                            inserted = true;
                        }
                    }
                });
                if (!inserted) {
                    table.find(".visit-table-body").append(newRow);
                }
            } else {
                table = jq(data).find(".visit-table");
                addLinksToVisitRow(table.find(".visit-table-row"), formName, encId, encTypeUuid);
                section.append(table);
            }

            if (showVisitTable) {
                jq('.flowsheet-edit-section').empty();
                flowsheet.toggleViewFlowsheet();
            }
        });
    };

    var addLinksToVisitRow = function(row, formName, encId, encTypeUuid) {
        if (!viewOnly) {
          // add Edit link only if the form's encounter type matches the encounter displayed (HTML-694)
          if (encTypeUuid === flowsheet.getEncounterTypeUuid(encId)) {
            var rowId = row.attr("id");
            if (!rowId || rowId.length == 0) {
              rowId = "visit-table-row"
            }
            row.attr("id", rowId + "-" + encId);

            var visitDateCell = jq(row).find(".visit-date");
            var existingDateCell = visitDateCell.html();
            var editLink = jq('<a href="#" onclick="flowsheet.editVisit(\'' + formName + '\', ' + encId + ');">' + existingDateCell + '</a>');
            visitDateCell.empty().append(editLink);
          }
        }
    };

    /**
     * Takes in a table row, expecting a td of class .visit-date with a view-mode encounter date div of class .value
     * Extracts this into a moment in order to compare with other rows.
     */
    flowsheet.extractVisitMoment = function(row) {
        var dateStr = jq(row).find(".visit-date .value").html();
        if (dateStr && dateStr.length > 0) {
            return moment(dateStr, "DD/MM/YYYY");
        }
    };

    /**
     * Loads the htmlform for a given encounter, calling the function passed in as 'action' with the htmlform results
     * If no existing encounter, can specify a default encounter date; should never specify both encounterId and encounterDate parameters
     */
    var loadHtmlFormForEncounter = function(formName, encounterId, encounterDate, editMode, action) {
        jq.get(emr.pageLink('htmlformentryui', 'htmlform/htmlForm', {
            "patient": patientId,
            "encounter": encounterId,
            "editMode": editMode,
            "formName": formName,
            "encounterDate": encounterDate != null ? encounterDate : "",
        }), action).always( function() {
            var index = loadingEncounters.indexOf(encounterId);
            if (index > -1) {
                // the encounter visit was successfully loaded into the table
                loadingEncounters.splice(index, 1);
            }
        });
    };

    var showLinksForEditMode = function() {
        jq(".form-action-link").hide();
        if (currentlyEditingEncounterId != null) {
            jq("#delete-button").show();
        }
        jq("#cancel-button").show();
    };

    var setupForm = function(html) {
        validationErrors = {};
        showLinksForEditMode();

        // Configure warning if navigating away from form
        jq(html).find(':input').change(function () {
            flowsheet.setDirty();
        });

        // Set up togglable fields
        jq(html).find("[data-toggle-source]").each(function() {
            var toggleCheckbox = jq(this).find("input:checkbox");
            var toggleTargetId = jq(this).data("toggle-target");
            var toggleTarget = jq("#"+toggleTargetId);
            jq(toggleCheckbox).change(function() {
                toggleEnabledDisabled(toggleTarget, jq(this).prop("checked"));
            });
            toggleEnabledDisabled(toggleTarget, jq(toggleCheckbox).prop("checked"));
        });

        if (flowsheet.getFlowsheetExtension()) {
            flowsheetExtension.afterSetupForm(flowsheet, html);
        }
    };

    var toggleEnabledDisabled = function(toggleTarget, enable) {
        if (enable) {
            jq(toggleTarget).find(":input").prop("disabled", false);
        }
        else {
            jq(toggleTarget).find(":input").prop("disabled", true).val("");
        }
    };

    function compare(a,b) {
        if ( a.encounterDateTime.isBefore(b.encounterDateTime) ) {
            return -1;
        }
        else if ( a.encounterDateTime.isAfter(b.encounterDateTime) ) {
            return 1;
        }
        else {
            return 0;
        }
    }

    flowsheet.toggleError = function(field, errorMessage) {
        var errorDiv = field.siblings(".field-error");
        if (errorMessage) {
            errorDiv.html(errorMessage).show();
            validationErrors[field.attr("id")] = errorMessage;
        }
        else {
            errorDiv.html("").hide();
            delete validationErrors[field.attr("id")];
        }
        if (flowsheet.isValidForSubmission()) {
            jq(".submitButton").removeAttr("disabled");
        }
        else {
            jq(".submitButton").attr("disabled","disabled");
        }
    };

}( window.flowsheet = window.flowsheet || {}, jQuery ));
