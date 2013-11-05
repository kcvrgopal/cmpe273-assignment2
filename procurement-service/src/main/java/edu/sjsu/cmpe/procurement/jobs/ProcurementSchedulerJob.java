package edu.sjsu.cmpe.procurement.jobs;

import java.util.Iterator;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;
import edu.sjsu.cmpe.procurement.domain.Book;

/**
 * This job will run at every 5 minutes.
 */
@SuppressWarnings("unused")
@Every("5mn")
public class ProcurementSchedulerJob extends Job {
	private final Logger log = LoggerFactory.getLogger(getClass());
	//Map<String,Book[]> collector;

	@SuppressWarnings("static-access")
	@Override
	public void doJob() {
		String body = "{\"id\":\"83806\",\"order_book_isbns\":[";
		String jsonBody = null;
		int count=0;


		try
		{
			long waitUntil = 500;
			//Getting the order requests from Queue
			while(true) 
			{
				Message msg = ProcurementService.consumer.receive(waitUntil);
				if (msg instanceof TextMessage)
				{
					body += ((TextMessage) msg).getText().substring(10)+",";
					count++;
				}
				else if (msg instanceof StompJmsMessage) {
					StompJmsMessage smsg = ((StompJmsMessage) msg);
					body+= smsg.getFrame().contentAsString().substring(10)+",";
					count++;
				} 	     
				else if (msg == null) 

				{
					System.out.println("msg is null");
					System.out.println("No new messages. Exiting due to timeout - " + waitUntil / 1000 + " sec");
					break; 
				}
				else
				{
					System.out.println("Unexpected message type: "+msg.getClass());
				}
			}
			body+="]}";
			if(count>0)
			{
				int position=body.lastIndexOf(",");
				jsonBody = body.substring(0,position)+""+body.substring(position+1);
				System.out.println("Posted order = " + jsonBody);
				//Posting the orders to publisher
				ClientResponse strResponse=ProcurementService.jerseyClient.create().resource(
						"http://54.215.210.214:9000/orders").type("application/json").post(ClientResponse.class,jsonBody);    	
				log.debug("Response from POST request to Publisher:{}", strResponse.getEntity(String.class));
			}
			else
			{
				System.out.println("No new orders to request publisher!!!");
			}

			//NOW GET FROM PUBLISHER AND POST TO TOPIC
			ClientResponse responseGet=ProcurementService.jerseyClient.create().resource(
					"http://54.215.210.214:9000/orders/83806").accept("application/json").get(ClientResponse.class);
			Map<String,Book[]> collector;
			if (responseGet.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ responseGet.getStatus());
			}
			System.out.println("Output from Server .... \n");
			collector = responseGet.getEntity(new GenericType<Map<String, Book[]>>(){}) ;

			Book[] shipped_books = collector.get("shipped_books");
			System.out.println("Received "+shipped_books.length+" books from publisher!");

			//Logic to segregate topics based on category
			for(int i=0;i<shipped_books.length;i++)
			{  	

				if(shipped_books[i].getCategory().equalsIgnoreCase("computer"))
				{
					String data=shipped_books[i].getIsbn()+":\""+shipped_books[i].getTitle()+"\":\""+shipped_books[i].getCategory()+"\":\""+shipped_books[i].getCoverimage()+"\"";
					ProcurementService.computer(data);
				}
				else if(shipped_books[i].getCategory().equalsIgnoreCase("management"))
				{
					String data=shipped_books[i].getIsbn()+":\""+shipped_books[i].getTitle()+"\":\""+shipped_books[i].getCategory()+"\":\""+shipped_books[i].getCoverimage()+"\"";
					ProcurementService.management(data);
				}
				else if(shipped_books[i].getCategory().equalsIgnoreCase("selfimprovement"))
				{
					String data=shipped_books[i].getIsbn()+":\""+shipped_books[i].getTitle()+"\":\""+shipped_books[i].getCategory()+"\":\""+shipped_books[i].getCoverimage()+"\"";
					ProcurementService.selfimprovement(data);
				}
				else
				{
					String data=shipped_books[i].getIsbn()+":\""+shipped_books[i].getTitle()+"\":\""+shipped_books[i].getCategory()+"\":\""+shipped_books[i].getCoverimage()+"\"";
					ProcurementService.comics(data);
				}
			}

		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}

}
