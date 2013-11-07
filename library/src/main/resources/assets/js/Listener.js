$(document).ready(function() {  
	if(window.WebSocket) {
		var url = "ws://54.215.210.214:61623";
		var login = "admin";
		var passcode = "password";
		var destination = "/topic/83806.book.*";
		var destination2="/topic/83806.book.computer"; 	
		if (location.port==8001)
		{
			var destination = "/topic/83806.book.*";
		}
		else 
		{
			var destination = "/topic/83806.book.computer";
		}  

		client = Stomp.client(url, "stomp");
		client.connect(login, passcode, function() {
			client.debug("connected to Stomp");
			//alert("connected");
			var sub1 = client.subscribe(destination, function(message) {
				//alert(message);
				var res=message.body;
				var data = res.split(":"); 
				var dataRes = "{ \"isbn\" :"+data[0]+",\"title\" :"+data[1]+",\"category\" :"+data[2]+",\"coverimage\" :"+data[3]+":"+data[4]+"}";
				//alert(dataRes);
				$.ajax({
					url: "/library/v1/books",
					type: 'POST',
					data:dataRes,
					contentType: 'application/json',
					success: function(result) {
						//alert("Post New Book successful");
						window.location.reload();
					}
				});

			});
		});
	}
});  

