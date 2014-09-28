var l;

var main = function() {
    Ladda.bind( 'input[type=button]' );
    $('.go-btn').click(query);

    $('#query').keyup(function(event){
	if(event.keyCode == 13) { // Enter
	    query();
	}
    });
    
    if (window.location.hash) {
	var queryStr = window.location.hash.substring(1);
	$('#query').val(queryStr);
	query();
    }
};

var showAnswers = function(data) {
    $.get('template.txt', function(answerTemplate) {
	l.stop();
	try {
	    template = jsontemplate.Template(answerTemplate);
	    html = template.expand(data);
	    $('#results').html(html);
	    Prism.highlightAll();

	    $('#details').hide();
	    $('#dropdown-details').click(function() {
		$input = $( this );
		$target = $('#details');
		$target.slideToggle();
	    });

	    svg = Viz(data.dependency_tree, "svg");
	    $('#dependency_tree').html(svg);
	    width = parseInt($('svg').attr("width"));
	    height = parseInt($('svg').attr("height"));
	    $('svg').attr("width", width*0.8);
	    $('svg').attr("height", height*0.8);
	    $('text').attr("font-family", "arial");
	    $('polygon').first().attr("fill", "none");
	} catch (err) {
	    alert("Failed to fill HTML template with JSON response from server. " + err.name + ": " + err.message);
	}
    });
};

var query = function() {
    var queryStr = $('#query').val(); 
    l = Ladda.create($('.ladda-button')[0]);
    l.start();
    var sparqlEndpoint = $("input:radio[name='p']:checked").val();
    var queryUrl = 'ask?p=' + encodeURIComponent(sparqlEndpoint) + '&q=' + encodeURIComponent(queryStr);
    //var queryUrl = '/ask.json';
    $('#results').html("");
    try {
        $.getJSON(queryUrl, showAnswers)
        	.fail(function(jqXHR, textStatus, errorThrown) { l.stop(); alert("Error retrieving JSON data (" + queryUrl + "): " + errorThrown); });
    } catch (err) {
	l.stop();
	alert("Error retrieving JSON data (URL " + queryUrl + "): " + err);
    }
};


$(document).ready(main);