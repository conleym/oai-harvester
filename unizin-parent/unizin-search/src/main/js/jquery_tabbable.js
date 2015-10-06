/* eslint-disable */
var $ = require('jquery');

$.expr.filters.hidden = function(elem) {
	var rect = elem.getBoundingClientRect();
	return rect.height === 0 || rect.width === 0;
};

// This file was copied directly from jQueryUI. It provides $(':tabbable') and
// $(':focusable')
// https://github.com/jquery/jquery-ui/blob/0de27b0609e8f6f9751ab7cce28492e18206d86d/ui/core.js#L133-L182
/*eslint indent: [2, "tab"] */
/*eslint-disable func-style */
/*eslint-disable yoda */
/*eslint-disable no-use-before-define */
function focusable( element, hasTabindex ) {
	var map, mapName, img,
		nodeName = element.nodeName.toLowerCase();

	if ( "area" === nodeName ) {
		map = element.parentNode;
		mapName = map.name;
		if ( !element.href || !mapName || map.nodeName.toLowerCase() !== "map" ) {
			return false;
		}
		img = $( "img[usemap='#" + mapName + "']" )[ 0 ];
		return !!img && visible( img );
	}
	return ( /^(input|select|textarea|button|object)$/.test( nodeName ) ?
		!element.disabled :
		"a" === nodeName ?
			element.href || hasTabindex :
			hasTabindex ) &&
		// the element and all of its ancestors must be visible
		visible( element );
}

function visible( element ) {
	return $.expr.filters.visible( element ) &&
		!$( element ).parents().addBack().filter( function() {
			return $.css( this, "visibility" ) === "hidden";
		} ).length;
}

$.extend( $.expr[ ":" ], {
	data: $.expr.createPseudo ?
		$.expr.createPseudo( function( dataName ) {
			return function( elem ) {
				return !!$.data( elem, dataName );
			};
		} ) :
		// support: jQuery <1.8
		function( elem, i, match ) {
			return !!$.data( elem, match[ 3 ] );
		},

	focusable: function( element ) {
		return focusable( element, $.attr( element, "tabindex" ) != null );
	},

	tabbable: function( element ) {
		var tabIndex = $.attr( element, "tabindex" ),
			hasTabindex = tabIndex != null;
		return ( !hasTabindex || tabIndex >= 0 ) && focusable( element, hasTabindex );
	}
} );

export default $
