var l;

var main = function() {
	Ladda.bind( 'input[type=button]' );
	$('.go-btn').click(function() {
	 	l = Ladda.create(this);
	 	l.start();
		var queryStr = $('#query').val();
		query(queryStr);
	});
};

var query = function(queryStr) {
	var queryUrl = 'http://localhost:8080/ask?q=' + encodeURIComponent(queryStr);
	//var queryUrl = 'http://localhost:8080/ask.json';
	$.getJSON(queryUrl, function(data) {
		$.get('http://localhost:8080/template.txt', function(answerTemplate) {
			template = jsontemplate.Template(answerTemplate);
			html = template.expand(data);
			$('#results').html(html);
			l.stop();
		});
	});
};


$(document).ready(main);