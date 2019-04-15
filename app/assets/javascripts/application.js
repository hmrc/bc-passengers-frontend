/* global $ */
/* global jQuery */
/* global GOVUK */
/* Taken from https://github.com/alphagov/govuk_elements/blob/master/assets/javascripts/application.js */

$(document).ready(function () {
    // Turn off jQuery animation
    jQuery.fx.off = true;
    //Add keypress event on every link where exist role="button"
    $(document).on('keypress', 'a[role="button"]', null, onKeypressButton, false);

    // =====================================================
    // Details/summary polyfill from frontend toolkit
    // =====================================================
    GOVUK.details.init();

})

$(window).load(function () {
    // If there is an error summary, set focus to the summary
    if ($('.error-summary').length) {
        $('.error-summary').focus();
    }
})

// ================================================================================
//  Function to enhance any select element into an accessible auto-complete (by id)
// ================================================================================
function enhanceSelectIntoAutoComplete(selectElementId, dataSource) {

  accessibleAutocomplete.enhanceSelectElement({
    selectElement: document.querySelector('#' + selectElementId),
    displayMenu: 'inline',
    defaultValue: '',
    source: customSuggest,
    onConfirm: function(confirmed) {
      $('select[name="'+selectElementId+'"]').val(confirmed.code);
    },
    templates: {
      inputValue: function(result) {
        return result && result.displayName;
      },
      suggestion: function(result) {
        return result.displayName;
      }
    }
  })

  function customSuggest (query, syncResults) {
    var results = dataSource
    syncResults(query ? results.filter(function (result) {
      return (result.synonyms.findIndex( function(s) { return s.toLowerCase().indexOf(query.toLowerCase()) !== -1 } ) !== -1 ) || (result.displayName.toLowerCase().indexOf(query.toLowerCase()) !== -1)
    }) : [])
  }

}

function onKeypressButton(e) {
    // Need both, 'keyCode' and 'which' to work in all browsers.
    var code = e.keyCode || e.which,
    spaceKey = 32;
    //If user press space key:
    if (code == spaceKey) {
        // Do same thing as onclick:
        $(e.currentTarget)[0].click();
        e.preventDefault();
        e.stopImmediatePropagation();
    }
}


/**
 * detect IE
 * returns version of IE or false, if browser is not Internet Explorer
 * https://codepen.io/gapcode/pen/vEJNZN
 */
function detectIE() {
  var ua = window.navigator.userAgent;

  var trident = ua.indexOf('Trident/');
  if (trident > 0) {
    // IE 11 => return version number
    var rv = ua.indexOf('rv:');
    return parseInt(ua.substring(rv + 3, ua.indexOf('.', rv)), 10);
  }

  // other browser
  return false;
}