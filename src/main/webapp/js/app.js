var l;

var main = function() {
    Ladda.bind( 'input[type=button]' );
    $('.go-btn').click(function() {
	l = Ladda.create(this);
	l.start();
	var queryStr = $('#query').val();
	query(queryStr);
    });

    $('#query').keyup(function(event){
	if(event.keyCode == 13) { // Enter
	    $('.go-btn').click();
	}
    });
};

var showAnswers = function(data) {
    $.get('template.txt', function(answerTemplate) {
	template = jsontemplate.Template(answerTemplate);
	html = template.expand(data);
	$('#results').html(html);
	l.stop();
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
    });
};

var query = function(queryStr) {
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