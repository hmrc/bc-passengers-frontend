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
    }
})

// ================================================================================
//  Function to enhance any select element into an accessible auto-complete (by id)
// ================================================================================
function enhanceSelectIntoAutoComplete(selectElementId, dataSource) {

  accessibleAutocomplete.enhanceSelectElement({
    selectElement: document.querySelector('#' + selectElementId),
    name: selectElementId,
    displayMenu: 'inline',
    defaultValue: '',
    source: customSuggest,
    templates: {
      inputValue: function(result) {
        return result && result.countryName
      },
      suggestion: function(result) {
        return result.countryName
      }
    }
  })

  function customSuggest (query, syncResults) {
    var results = dataSource
    syncResults(query ? results.filter(function (result) {
      return (result.countrySynonyms.findIndex( function(s) { return s.toLowerCase().indexOf(query.toLowerCase()) !== -1 } ) !== -1 ) || (result.countryName.toLowerCase().indexOf(query.toLowerCase()) !== -1)
    }) : [])
  }
}

