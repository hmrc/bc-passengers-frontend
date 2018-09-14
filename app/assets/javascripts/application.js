/* global $ */
/* global jQuery */
/* global GOVUK */
/* Taken from https://github.com/alphagov/govuk_elements/blob/master/assets/javascripts/application.js */

$(document).ready(function () {
    // Turn off jQuery animation
    jQuery.fx.off = true

})

$(window).load(function () {
    // If there is an error summary, set focus to the summary
    if ($('.error-summary').length) {
        $('.error-summary').focus()
        $('.error-summary a').click(function (e) {
            e.preventDefault()
            $('.form-group-error input:first').focus()
        })
    }
})