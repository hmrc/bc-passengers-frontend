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

    GOVUK.shimLinksWithButtonRole.init()

    if (typeof GOVUK.ShowHideContent !== 'undefined') {
          var showHideContent = new GOVUK.ShowHideContent();
          showHideContent.init();
    }

    $('input[type=submit], button[type=submit]').data('ignore-double-submit', 'true');
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
function enhanceSelectIntoAutoComplete(selectElementId, dataSource, submitOnConfirm = false) {
  selectElementId = selectElementId.replace( /(:|\.|\[|\]|,|=)/g, "\\$1" )
  accessibleAutocomplete.enhanceSelectElement({
    selectElement: document.querySelector('#' + selectElementId),
    displayMenu: 'inline',
    minLength: 2,
    source: customSuggest,
    confirmOnBlur: true,
    onConfirm: function(confirmed) {

      //Workaround the bug sending confirmed = undefined when confirmOnBlur == true
      let foundInData = dataSource.find(e => e.displayName === $('#'+selectElementId).val())
      let element = !!confirmed ? confirmed : foundInData

      if(!!element) {
        $('select[name="'+selectElementId+'"]').val(element.code);
        if(submitOnConfirm) {
          window.setTimeout(function(){
            $('form').submit();
          }, 100);
        }
      }
      else {
        $('select[name="'+selectElementId+'"]').val('')
      }
    },
    templates: {
      inputValue: function(result) {
        return (!!result && result.displayName ? result.displayName : '');
      },
      suggestion: function(result) {
        return !!result.displayName ? result.displayName : result;
      }
    }
  })

  function customSuggest (query, syncResults) {
    var results = dataSource
    syncResults(query ? results.filter(function (result) {
      return (result.synonyms.findIndex( function(s) { return s.toLowerCase().indexOf(query.toLowerCase()) !== -1 } ) !== -1 ) || (result.displayName.toLowerCase().indexOf(query.toLowerCase()) !== -1)
    }) : [])
  }

  if (document.querySelector('#' + selectElementId)) {
     var selectDescribedByValues = $('#' + selectElementId+'-select').attr('aria-describedby');
     var selectLabeledByValues = $('#' + selectElementId+'-select').attr('aria-labelledby');
     if(selectDescribedByValues != undefined) {
        $(".autocomplete__wrapper #" + selectElementId).attr('aria-describedby', selectDescribedByValues);
     }
     if(selectLabeledByValues != undefined) {
        $(".autocomplete__wrapper #" + selectElementId).attr('aria-labelledby', selectLabeledByValues);
     }
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