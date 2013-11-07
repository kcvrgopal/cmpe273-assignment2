$(":button").click(function() {
	var isbn = this.id;
	alert('About to report lost on ISBN ' + isbn);
	$.ajax({
		url: "v1/books/"+isbn+"?status=lost",
		type: 'PUT',
		contentType: 'application/json',
		success: function(result) {
			document.getElementById(isbn+":status").innerHTML="lost";
			document.getElementById(isbn).disabled = true;
		}
	});
});