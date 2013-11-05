package edu.sjsu.cmpe.procurement;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.client.JerseyClientBuilder;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

import de.spinscale.dropwizard.jobs.JobsBundle;
import edu.sjsu.cmpe.procurement.api.resources.RootResource;
import edu.sjsu.cmpe.procurement.config.ProcurementServiceConfiguration;
import edu.sjsu.cmpe.procurement.domain.Book;
import edu.sjsu.cmpe.procurement.jobs.ProcurementSchedulerJob;

@SuppressWarnings("unused")
public class ProcurementService extends Service<ProcurementServiceConfiguration> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * FIXME: THIS IS A HACK!
     */
    public static Client jerseyClient;
    public static MessageConsumer consumer;
    public static MessageProducer producer;
    public static Session session;
    static String comptopic="/topic/83806.book.computer";
	static String mgmttopic="/topic/83806.book.management";
	static String comictopic="/topic/83806.book.comics";
	static String selftopic="/topic/83806.book.selfimprovement";
    
    public static void main(String[] args) throws Exception {
	new ProcurementService().run(args);
    }

    @Override
    public void initialize(Bootstrap<ProcurementServiceConfiguration> bootstrap) {
	bootstrap.setName("procurement-service");
	/**
	 * NOTE: All jobs must be placed under edu.sjsu.cmpe.procurement.jobs
	 * package
	 */
	bootstrap.addBundle(new JobsBundle("edu.sjsu.cmpe.procurement.jobs"));
    }
    private static String env(String key, String defaultValue) {
    	String rc = System.getenv(key);
    	if( rc== null ) {
    	    return defaultValue;
    	}
    	return rc;
        }
    @Override
    public void run(ProcurementServiceConfiguration configuration,
	    Environment environment) throws Exception {
	jerseyClient = new JerseyClientBuilder()
	.using(configuration.getJerseyClientConfiguration())
	.using(environment).build();
	System.out.println(configuration.getJerseyClientConfiguration());

	/**
	 * Root API - Without RootResource, Dropwizard will throw this
	 * exception:
	 * 
	 * ERROR [2013-10-31 23:01:24,489]
	 * com.sun.jersey.server.impl.application.RootResourceUriRules: The
	 * ResourceConfig instance does not contain any root resource classes.
	 */
	environment.addResource(RootResource.class);

	String queueName = configuration.getStompQueueName();
	String topicName = configuration.getStompTopicPrefix();
	String user = env("APOLLO_USER", "admin");
	String password = env("APOLLO_PASSWORD", "password");
	String host = env("APOLLO_HOST", "54.215.210.214");
	int port = Integer.parseInt(env("APOLLO_PORT", "61613"));
	String queue = queueName;
	String destination = queueName;

	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	factory.setBrokerURI("tcp://" + host + ":" + port);

	Connection connection = factory.createConnection(user, password);
	connection.start();
	session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	Destination dest = new StompJmsDestination(destination);
	 consumer = session.createConsumer(dest);

    }
    //Setting the topic for computer category
    public static void computer(String book)
    {
    	try {    	
		Destination topicdestination = new StompJmsDestination(comptopic);
		producer= session.createProducer(topicdestination);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		 msgSend(book);
    	} catch (JMSException e) {
			e.printStackTrace();
		}

    }
  //Setting the topic for management category
    public static void management(String book)
    {
    	try {    	
    		Destination topicdestination = new StompJmsDestination(mgmttopic);
    		producer= session.createProducer(topicdestination);
    		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    		 msgSend(book);
        	} catch (JMSException e) {
    			e.printStackTrace();
    		}
    	
    }
  //Setting the topic for comics category
    public static void comics(String book)
    {
    	try {    	
    		Destination topicdestination = new StompJmsDestination(comictopic);
    		producer= session.createProducer(topicdestination);
    		
    		 producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    		 msgSend(book);
        	} catch (JMSException e) {
    			e.printStackTrace();
    		}
    }
  //Setting the topic for selfimprovement category
    public static void selfimprovement(String book)
    {
    	try {    	
    		Destination topicdestination = new StompJmsDestination(selftopic);
    		producer= session.createProducer(topicdestination);
    		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
    		 msgSend(book);
        	} catch (JMSException e) {
    			e.printStackTrace();
    		}
    }
    
    //Sending the received books to the topic
    public static void msgSend(String book)
	{
		try
		{
					TextMessage msg = ProcurementService.session.createTextMessage(book);
					StompJmsMessage smsg=(StompJmsMessage) msg;
					smsg.setLongProperty("id", System.currentTimeMillis());
					ProcurementService.producer.send(smsg);
					System.out.println("Sending Books to topic"+book);
		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
}
