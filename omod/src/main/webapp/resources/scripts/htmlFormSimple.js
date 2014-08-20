(function( htmlForm, $, undefined) {

    htmlForm.simpleUi = {
        showSection: function(sectionId) {
            NavigatorController.getSectionById(sectionId).show();
        },

        hideSection: function(sectionId) {
            NavigatorController.getSectionById(sectionId).hide();
        },

        showQuestion: function(questionId) {
            NavigatorController.getQuestionById(questionId).show();
        },

        hideQuestion: function(questionId) {
            NavigatorController.getQuestionById(questionId).hide();
        },

        showField: function(fieldId) {
            NavigatorController.getFieldByContainerId(fieldId).show();
        },

        hideField: function(fieldId) {
            NavigatorController.getFieldByContainerId(fieldId).hide();
        }
    }

}( window.htmlForm = window.htmlForm || {}, jQuery ));