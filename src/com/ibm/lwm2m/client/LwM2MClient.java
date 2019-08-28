package com.ibm.lwm2m.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.lwm2m.objects.OtaObject;
import com.ibm.lwm2m.objects.AimDevObject;
import com.ibm.lwm2m.objects.AimAppObject;
import com.ibm.lwm2m.objects.RaspiDeviceObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.lwm2m.objects.LwM2MServerObject;
import com.ibm.lwm2m.objects.TemperatureSensorObject;
import com.ibm.mqttv3.binding.AbstractRequestObserver;
import com.ibm.mqttv3.binding.MQTTWrapper;
import com.ibm.mqttv3.binding.MqttV3MessageReceiver;
import com.ibm.mqttv3.binding.Request;
import com.ibm.mqttv3.binding.Resource;
import com.ibm.mqttv3.binding.Response;
import com.ibm.lwm2m.utils.SystemUtil;
import com.ibm.lwm2m.objects.AdvDeviceObject;
import com.ibm.lwm2m.objects.ExtendInfoObject;
import com.ibm.lwm2m.utils.ClientReportRegistry;
import com.ibm.lwm2m.utils.ClientReportListener;
import com.ibm.lwm2m.utils.CommTrans;

public class LwM2MClient {
	/*
	 * Solicited interactions require a request/response message pattern to be
	 * established over MQTT. To initiate a solicited conversation, a remote
	 * server first sends a request message to a given application running on a
	 * specific device and then waits for a response. To ensure the delivery of
	 * request messages, applications that support request/response
	 * conversations via MQTT should subscribe to the following topic on
	 * startup:
	 * 
	 * LWM/?/account_name/server_id/app_id/#
	 * 
	 * $ - represents whether its a Request (S) or response (R) So Every client
	 * and server must subscribe to the following topic during startup
	 * 
	 * LWMS/account_name/server_id/app_id/#
	 */

	public static final String RESPONSE_TOPIC_STARTER = "LWM/R";
	public static final String REQUEST_TOPIC_STARTER = "LWM/S";
	private static final int REQUEST_TIMEOUT_MILLIS = 30000;//5000;
	private static final int OTA_REPORT_TIMEOUT_MILLIS = 30000;

	private static final byte QOS = 2;

	private static final Logger LOG = LoggerFactory
			.getLogger(LwM2MClient.class);

	private static LwM2MClient client = null;
	private MQTTWrapper mqttClient;
	private MqttV3MessageReceiver callback;
	private String serverEndpointId;
	private String serverApplicationId;
	private String clientEndpointId;
	private String clientApplicationId;
	private String orgId;
	private String registerLocationID;
	private String mqtt_username;
	private String mqtt_passwd;
	private String hostname;
	private int portNumber;
	private static ClientReportRegistry  clientReportRegistry = new ClientReportRegistry();
	private final ClientReportListener listener =  new ClientReportListener() {
		@Override
		public boolean reportAndWaitforRep(String dpName, String type, String version, int status, int updstatus, int errcode) {
			boolean ret = false;
			int tryCnt = 3;
			for(int i=0; i<3; i++) {
				ret = client.reportOtaStatus(dpName, type, version, status, updstatus, errcode);
				if(ret == true)
					break;
			}
			return ret;
		}
	};

	public static ClientReportRegistry getClientReportRegistry(){
		return clientReportRegistry;
	}

	public void addHook(){
		clientReportRegistry.addListener(this.listener);
	}

	private LwM2MClient() {
		serverEndpointId = "10"; // assumed to come from server as part of
									// bootstrap in the LwM2M server object
		serverApplicationId = "leshan-server"; // assumed to come from server as
												// part of bootstrap in the
												// LwM2M server object
		clientEndpointId = "20";
		clientApplicationId = "mqtt-client";
		orgId = "eclipse";

	}
	public String getWillTopic(){
		Request mqttRequest = Request.newDelete();
		mqttRequest.setRequestorApplicationID(this.clientApplicationId);
		mqttRequest.setRequestorEndpointID(this.clientEndpointId);
		mqttRequest.setApplicationID(this.serverApplicationId);
		mqttRequest.setEndPointId(this.serverEndpointId);
		mqttRequest.setOrganizationID(this.orgId);
		mqttRequest.addURIPath("rd");
		mqttRequest.addURIPath("willmessage");

		return mqttRequest.getTopic();

	}
	public String getWillMsg(){
		Request mqttRequest = Request.newDelete();
		mqttRequest.setRequestorApplicationID(this.clientApplicationId);
		mqttRequest.setRequestorEndpointID(this.clientEndpointId);
		mqttRequest.setApplicationID(this.serverApplicationId);
		mqttRequest.setEndPointId(this.serverEndpointId);
		mqttRequest.setOrganizationID(this.orgId);
		mqttRequest.addURIPath("rd");

		return mqttRequest.getMessageAsString();

	}
	private void subscribeTops(){
		StringBuilder topic = new StringBuilder();
		topic.append("LWM/+/").append(orgId).append("/")
				.append(clientEndpointId).append("/")
				.append(clientApplicationId).append("/#");
		// Subscribe a topic to broker
		mqttClient.subscribe(topic.toString(), QOS);
	}
	public void reConnect(Boolean ifRegiste){
		// must set receive callback here
		//status = CONN_STATUS_RECONNECTED;
		callback = new MqttV3MessageReceiver(mqttClient);
		mqttClient.setCallBack(callback);
		subscribeTops();
		if(ifRegiste) {
			updateRegisteration();
		}
		return;
	}

	boolean start() {
		Properties properties = new Properties();

		InetSocketAddress mqttBrokerAddress = new InetSocketAddress(
				"localhost", 1883);
		try {
			properties.load(Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("mqtt.properties"));
			serverEndpointId = properties.getProperty("SERVER_ID");
			serverApplicationId = properties
					.getProperty("SERVER_APPLICATIONID");

			clientEndpointId = SystemUtil.getMACAddress();
			CommTrans.setDeviceId(clientEndpointId);
			clientApplicationId = properties
					.getProperty("CLIENT_APPLICATIONID");

			orgId = properties.getProperty("ORGID");

			hostname = properties.getProperty("MQTT_SERVER");
			portNumber = Integer.parseInt(properties
					.getProperty("MQTT_PORT"));
			mqttBrokerAddress = new InetSocketAddress(hostname, portNumber);
			mqtt_username = properties.getProperty("MQTT_USERNAME");
			mqtt_passwd = properties.getProperty("MQTT_PWD");
		} catch (IOException e) {
			e.printStackTrace();
		}
		mqttClient = new MQTTWrapper(mqttBrokerAddress, clientEndpointId, mqtt_username, mqtt_passwd);

		// Register to MQTT server
		String serverURI = "tcp://"+hostname+":"+portNumber;
		LOG.info("mqttserver:"+hostname+"/port:"+portNumber);

		// Create a server message receiver and set the callback
		callback = new MqttV3MessageReceiver(mqttClient);

		// Register to MQTT server
		Boolean isMqttStart = mqttClient.start(serverURI, this, callback);
		if(!isMqttStart){
			return false;
		}


		StringBuilder topic = new StringBuilder();
		topic.append("LWM/+/").append(orgId).append("/")
				.append(clientEndpointId).append("/")
				.append(clientApplicationId).append("/#");
		// Subscribe a topic to broker
		mqttClient.subscribe(topic.toString(), QOS);

		// Create an instanceof Lwserver Object - assume that its sent by server
		// during bootstrap
		LwM2MServerObject.createObject();
		LwM2MServerObject.createObjectInstance();

		// set the server id
		setServerId(serverEndpointId);

		// Create the temperature object that this client is representing
		AdvDeviceObject.createObject();
		AdvDeviceObject.createObjectInstance();

		ExtendInfoObject.createObject();
		ExtendInfoObject.createObjectInstance();

		AimDevObject.createObject();
		AimDevObject.createObjectInstance();

		AimAppObject.createObject();
		AimAppObject.createObjectInstance();

		OtaObject.createObject();
		OtaObject.createObjectInstance(1);

		//listObjs();

		return true;
	}
	private void listObjs(){
		StringBuilder result = new StringBuilder();
		Resource resource = this.mqttClient.getRoot();
		Collection<Resource> objects = resource.getChildren();
		if (objects == null)
			return;
		Iterator<Resource> itr = objects.iterator();
		while (itr.hasNext()) {
			Resource object = itr.next();
			String objName = object.getName();
			String instanceName = null;
			String resourceName = null;
			Collection<Resource> instances = object.getChildren();
			if (instances == null) {
				continue;
			} else {
				Iterator<Resource> instanceIterator = instances.iterator();
				while (instanceIterator.hasNext()) {
					Resource objectInstance = instanceIterator.next();
					instanceName = objectInstance.getName();
					Collection<Resource> resources = objectInstance.getChildren();
					if(resources == null){
						continue;
					}
					Iterator<Resource> resourceIterator = resources.iterator();
					while (resourceIterator.hasNext()){
						Resource res = resourceIterator.next();
						if(res == null) {
							continue;
						}
						// do clear logic
						resourceName = res.getName();
						LOG.info("list obj : /" + objName + "/" + instanceName + "/" + resourceName );
					}
				}
			}
		}
	}
	private void clearObjs(){
		StringBuilder result = new StringBuilder();
		Resource resource = this.mqttClient.getRoot();
		Collection<Resource> objects = resource.getChildren();
		if (objects == null)
			return;
		Iterator<Resource> itr = objects.iterator();
		while (itr.hasNext()) {
			Resource object = itr.next();
			String objName = object.getName();
			String instanceName = null;
			String resourceName = null;
			Collection<Resource> instances = object.getChildren();
			if (instances == null) {
				continue;
			} else {
				Iterator<Resource> instanceIterator = instances.iterator();
				while (instanceIterator.hasNext()) {
					Resource objectInstance = instanceIterator.next();
					instanceName = objectInstance.getName();
					Collection<Resource> resources = objectInstance.getChildren();
					if(resources == null){
						continue;
					}
					Iterator<Resource> resourceIterator = resources.iterator();
					while (resourceIterator.hasNext()){
						Resource res = resourceIterator.next();
						if(res == null) {
							continue;
						}
						// do clear logic
						resourceName = res.getName();
						LOG.info("clear obj : /" + objName + "/" + instanceName + "/" + resourceName );
						res.clear();
					}
				}
			}
		}
	}
	public void stop(){
		clearObjs();
		mqttClient.destroy();
	}

	public boolean reportOtaStatus(String dpName, String type, String version, int status, int updstatus, int errcode){
		LOG.info("reportOtaStatus, dpname="+dpName+"type="+type+" version="+version+" status="+status+" updstatus="+updstatus+" errcode="+errcode);
		// build a new post request
		Request mqttRequest = Request.newPost();
		mqttRequest.setRequestorApplicationID(this.clientApplicationId);
		mqttRequest.setRequestorEndpointID(this.clientEndpointId);
		mqttRequest.setApplicationID(this.serverApplicationId);
		mqttRequest.setEndPointId(this.getServerId());
		mqttRequest.setOrganizationID(this.orgId);

		// we know that the deploystatus ResourceName is "dpstus"
		mqttRequest.addURIPath("dpstus");

		StringBuilder sb = new StringBuilder();
		sb.append("ep=");
		sb.append(this.clientEndpointId);
		sb.append("&name=");
		sb.append(dpName);
		sb.append("&tp=");
		sb.append(type);
		sb.append("&ver=");
		sb.append(version);
		sb.append("&st=");
		sb.append(status);
		sb.append("&updst=");
		sb.append(updstatus);
		sb.append("&ecd=");
		sb.append(errcode);
		mqttRequest.setPayloadContent(sb.toString());

		// Add a message observer where the response will be notified
		final SyncRequestObserver syncMessageObserver = new SyncRequestObserver(
				mqttRequest, this.callback, OTA_REPORT_TIMEOUT_MILLIS) {
			private Response mqttResponse;

			@Override
			public void consumeResponse(final Response mqttResponse) {
				this.mqttResponse = mqttResponse;
			}

			@Override
			public Response getResponse() {
				waitForResponse();
				return mqttResponse;
			}

		};
		callback.addRequest(mqttRequest.getMessageID(), syncMessageObserver);
		mqttClient.publish(mqttRequest.getTopic(),
				mqttRequest.getMessageAsString());

		// Wait for response, then return it
		Response registerResponse = syncMessageObserver.getResponse();
		if(registerResponse == null){
			return false;
		}
		if(com.ibm.mqttv3.binding.ResponseCode.CHANGED.value == registerResponse.getCode())
			return true;
		else
			return false;
	}

	public boolean register() {


		String objects = getListofObjects();

		String ipAddress = SystemUtil.getIP();
		String boardName = SystemUtil.getSystemBoard();

		// build a new post request
		Request mqttRequest = Request.newPost();
		mqttRequest.setRequestorApplicationID(this.clientApplicationId);
		mqttRequest.setRequestorEndpointID(this.clientEndpointId);
		mqttRequest.setApplicationID(this.serverApplicationId);
		mqttRequest.setEndPointId(this.getServerId());
		mqttRequest.setOrganizationID(this.orgId);

		// we know that the registeration is done at the rd resource
		mqttRequest.addURIPath("rd");

		// Add the registeration parameters and objects as payload contents
		StringBuilder sb = new StringBuilder();
		sb.append("ep=");
		sb.append(this.clientEndpointId);
		// Add ip address
		sb.append("&ip=");
		sb.append(ipAddress);
		// Add board name
		sb.append("&bdnm=");
		sb.append(boardName);
		sb.append("&lt=").append("86400");
		sb.append(" ").append(objects);
		mqttRequest.setPayloadContent(sb.toString());

		// Add a message observer where the response will be notified
		final SyncRequestObserver syncMessageObserver = new SyncRequestObserver(
				mqttRequest, this.callback, REQUEST_TIMEOUT_MILLIS) {
			private Response mqttResponse;

			@Override
			public void consumeResponse(final Response mqttResponse) {
				this.mqttResponse = mqttResponse;
			}

			@Override
			public Response getResponse() {
				waitForResponse();
				return mqttResponse;
			}

		};
		callback.addRequest(mqttRequest.getMessageID(), syncMessageObserver);
		mqttClient.publish(mqttRequest.getTopic(),
				mqttRequest.getMessageAsString());

		// Wait for response, then return it
		Response registerResponse = syncMessageObserver.getResponse();
		if(registerResponse == null){
			return false;
		}
		this.registerLocationID = registerResponse.getPayloadText();

		// Deregister on shutdown and stop client.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				if (registerLocationID != null) {
					LOG.info("Device: Deregistering Client '"
							+ registerLocationID + "'");
					deregister();
					mqttClient.destroy();
				}
			}
		});
		return true;
	}

	public void updateRegisteration() {
		LOG.info("updateRegisteration");
		String objects = getListofObjects();
		String ipAddress = SystemUtil.getIP();
		String boardName = SystemUtil.getSystemBoard();

		// build a new post request
		Request mqttRequest = Request.newPost();
		mqttRequest.setRequestorApplicationID(this.clientApplicationId);
		mqttRequest.setRequestorEndpointID(this.clientEndpointId);
		mqttRequest.setApplicationID(this.serverApplicationId);
		mqttRequest.setEndPointId(this.getServerId());
		mqttRequest.setOrganizationID(this.orgId);

		// we know that the registeration is done at the rd resource
		mqttRequest.addURIPath("rd");

		// Add the registeration parameters and objects as payload contents
		StringBuilder sb = new StringBuilder();
		sb.append("ep=");
		sb.append(this.clientEndpointId);
		// Add ip address
		sb.append("&ip=");
		sb.append(ipAddress);
		// Add board name
		sb.append("&bdnm=");
		sb.append(boardName);
		sb.append("&lt=").append("86400");
		sb.append(" ").append(objects);
		mqttRequest.setPayloadContent(sb.toString());
		// Add a message observer where the response will be notified
		final SyncRequestObserver syncMessageObserver = new SyncRequestObserver(
				mqttRequest, this.callback, REQUEST_TIMEOUT_MILLIS) {
			private Response mqttResponse;

			@Override
			public void consumeResponse(final Response mqttResponse) {
				this.mqttResponse = mqttResponse;
			}

			@Override
			public Response getResponse() {
				waitForResponse();
				return mqttResponse;
			}

		};
		callback.addRequest(mqttRequest.getMessageID(), syncMessageObserver);
		mqttClient.publish(mqttRequest.getTopic(),
				mqttRequest.getMessageAsString());
		// Wait for response, then return it
		Response registerResponse = syncMessageObserver.getResponse();
		if(registerResponse == null){
			return;
		}
		this.registerLocationID = registerResponse.getPayloadText();
		return;

	}

	public void deregister() {

		// build a new delete request
		Request mqttRequest = Request.newDelete();
		mqttRequest.setRequestorApplicationID(this.clientApplicationId);
		mqttRequest.setRequestorEndpointID(this.clientEndpointId);
		mqttRequest.setApplicationID(this.serverApplicationId);
		mqttRequest.setEndPointId(this.getServerId());
		mqttRequest.setOrganizationID(this.orgId);

		mqttRequest.addURIPath(this.registerLocationID);
		mqttClient.publish(mqttRequest.getTopic(),
				mqttRequest.getMessageAsString());
		registerLocationID = null;
	}

	private String getListofObjects() {
		StringBuilder result = new StringBuilder();
		Resource resource = this.mqttClient.getRoot();
		Collection<Resource> objects = resource.getChildren();
		if (objects == null)
			return "";
		Iterator<Resource> itr = objects.iterator();
		while (itr.hasNext()) {
			Resource object = itr.next();
			String objName = object.getName();
			Collection<Resource> instances = object.getChildren();
			if (null == instances) {
				result.append("</").append(objName).append(">,");
			} else {
				Iterator<Resource> instanceIterator = instances.iterator();
				while (instanceIterator.hasNext()) {
					result.append("</").append(objName);
					Resource objectInstance = instanceIterator.next();
					result.append("/");
					result.append(objectInstance.getName());
					result.append(">,");
				}
			}
		}
		result.deleteCharAt(result.length() - 1);
		LOG.debug("List of objects exported by Client :: " + result.toString());
		return result.toString();
	}

	Resource getResource(String path) {
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		String ids[] = path.split("/");
		Resource resource = this.mqttClient.getRoot();

		if (ids.length >= 1 && ids[0] == null) {
			return null;
		} else {
			resource = resource.getChild(ids[0]);
		}

		if (ids.length >= 2 && ids[1] != null && resource != null) {
			resource = resource.getChild(ids[1]);
		}

		if (ids.length >= 3 && ids[2] != null && resource != null) {
			resource = resource.getChild(ids[2]);
		}

		return resource;
	}

	private static void list() {
		System.out.println("List of available commands");
		System.out.println(" register :: Register this client to server");
		System.out
				.println(" deregister :: deregister this client from the server");
		System.out.println(" update-register :: updates the registeration");
		System.out
				.println(" update :: (update <resource-id> <value>) update a local resource value");
		System.out
				.println(" get :: (get <resource-id>) get a local resource value");
		System.out.println(" >> Available Object and resource ");
		System.out.println(" >> IPSO temperature Object - <3303/0> ");
		System.out.println(" >> 3303/0/5700 - SensorValue ");
		System.out.println(" >> 3303/0/5601 - Minimum Measured Value ");
		System.out.println(" >> 3303/0/5602 - Maximum Measured Value ");
		System.out.println(" >> 3303/0/5603 - Min Range Value ");
		System.out.println(" >> 3303/0/5604 - Max Range Value ");
	}

	void userAction() {
		list();
		Scanner in = new Scanner(System.in);
		while (true) {
			System.out.println("Enter the command ");
			String input = in.nextLine();

			String[] parameters = input.split(" ");

			try {
				switch (parameters[0]) {

				case "register":
					if (registerLocationID == null) {
						this.register();
					} else {
						System.out.println("This client is already registered in the server");
					}
					break;

				case "deregister":
					if (registerLocationID != null) {
						this.deregister();
					} else {
						System.out.println("This client is either not registered "
								+ "or already de-registered from the server");
					}
					in.close();
					System.exit(0);
					break;

				case "update-register":
					this.updateRegisteration();
					break;

				case "get":
					if (parameters.length == 2) {
						LocalResource resource = ((LocalResource) getResource(parameters[1]));
						if (resource != null) {
							System.out.println(resource.getValue());
						} else {
							System.out.println("Resource - " + parameters[1]
									+ " not found!");
						}

					} else {
						System.out
								.println("Please specify the command as get <resource-id>");
					}
					break;

				case "update":
					if (parameters.length == 3) {
						LocalResource resource = (LocalResource) getResource(parameters[1]);
						if (resource != null) {
							resource.setValue(parameters[2]);
						} else {
							System.out.println("Resource - " + parameters[1]
									+ " not found!");
						}
					} else {
						System.out
								.println("Please specify the command as update <resource-id> <value>");
					}
					break;

				default:
					list();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	private String getServerId() {
		Resource resource = mqttClient.getRoot();
		// Get the server object
		resource = resource.getChild("1");
		// Get the server object instance
		resource = resource.getChild("0");
		// get the server id resource
		resource = resource.getChild("0");
		return ((LocalResource) resource).getValue();
	}

	private void setServerId(String id) {
		Resource resource = mqttClient.getRoot();
		// Get the server object
		resource = resource.getChild("1");
		// Get the server object instance
		resource = resource.getChild("0");
		// get the server id resource
		resource = resource.getChild("0");
		((LocalResource) resource).setValue(id);
	}

	public synchronized static LwM2MClient getClient() {
		if(client == null) {
			client = new LwM2MClient();
		}
		return client;
	}

	public static void releaseClient(){
		client = null;
	}

	public static Resource getRootResource() {
		return client.mqttClient.getRoot();
	}

	private abstract class SyncRequestObserver<T> extends
			AbstractRequestObserver {

		protected CountDownLatch latch = new CountDownLatch(1);
		protected AtomicBoolean mqttTimeout = new AtomicBoolean(false);
		protected AtomicReference<RuntimeException> exception = new AtomicReference<>();

		protected long timeout;
		protected MqttV3MessageReceiver messageObserver;

		public SyncRequestObserver(final Request mqttRequest,
				MqttV3MessageReceiver messageObserver, final long timeout) {
			super(mqttRequest);
			this.timeout = timeout;
			this.messageObserver = messageObserver;
		}

		public Response getResponse() {
			return null;
		}

		public abstract void consumeResponse(Response mqttResponse);

		@Override
		public void onResponse(final Response mqttResponse) {
			LOG.debug("Received response: " + mqttResponse);
			try {
				consumeResponse(mqttResponse);
			} catch (final RuntimeException e) {
				exception.set(e);
			} finally {
				latch.countDown();
			}
		}

		@Override
		public void onError(final Response mqttResponse) {
			try {
				consumeResponse(mqttResponse);
			} catch (final RuntimeException e) {
				exception.set(e);
			} finally {
				latch.countDown();
			}
		}

		public void waitForResponse() {
			try {
				final boolean latchTimeout = latch.await(timeout,
						TimeUnit.MILLISECONDS);
				if (!latchTimeout || mqttTimeout.get()) {
					if (exception.get() != null) {
						throw exception.get();
					} else {
						throw new RuntimeException("Request Timed Out: "
								+ mqttRequest + " (timeout)");
					}
				}
			} catch (final InterruptedException e) {
				LOG.error("Caught an unexpected InterruptedException during execution of CoAP request "
						+ e);
			} finally {
				if (messageObserver.getRequest(mqttRequest.getMessageID()) instanceof SyncRequestObserver)
					messageObserver.removeRequest(mqttRequest.getMessageID());
			}

			if (exception.get() != null) {
				throw exception.get();
			}
		}
	}

}
