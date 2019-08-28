package com.ibm.mqttv3.binding;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.ibm.lwm2m.utils.SystemUtil;
import com.ibm.lwm2m.utils.DESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.ibm.lwm2m.utils.DESUtil;
import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.mqttv3.binding.MQTT.Operation;

public class MqttV3MessageReceiver implements MqttCallbackExtended {

	private MQTTWrapper mqttClient;
	private Long deregisteBaseTime = System.currentTimeMillis();
	private static final Logger LOG = LoggerFactory.getLogger(MqttV3MessageReceiver.class);
	private static ConcurrentHashMap<Long, AbstractRequestObserver> requestObservers = new ConcurrentHashMap();
	private static ScheduledThreadPoolExecutor executor = 
			new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 1);
	
	public MqttV3MessageReceiver(MQTTWrapper mqttClient) {
		this.mqttClient = mqttClient;
	}
	
	@Override
	public void messageArrived(final String topic, final MqttMessage message)
			throws Exception {
		
		executor.execute(new Runnable() {
			public void run() {
				try {
					handleMessage(topic, message);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}

	@Override
	public void connectionLost(Throwable cause) {
		deregisteBaseTime = System.currentTimeMillis();
		LOG.info("Connection lost: "+cause.getMessage());
		cause.printStackTrace();

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//System.out.println("DeliveryComplte :: "+token.toString());

	}
	@Override
	public void connectComplete(boolean reconnect, String serverURI) {
		LOG.info("connectComplete, reconnect= "+ reconnect + ",url=" + serverURI);
		LwM2MClient client = LwM2MClient.getClient();
		if(reconnect == true) {
			Long currentTime = System.currentTimeMillis();
			Boolean ifRegiste = true;
			client.reConnect(ifRegiste);
		}
	}

	protected void handleMessage(String topic, MqttMessage message) {
		//LOG.info("MSG { "+topic + " ["+message.toString()+"]}");
		try {
			String content = new String(message.getPayload(), "UTF-8");
			LOG.info("MSG { "+topic + " ["+DESUtil.decrypt(content)+"]}");
			MqttMessage decryptedMesg = new MqttMessage(DESUtil.decrypt(content).getBytes("UTF-8"));

			if (topic.startsWith(Request.RESPONSE_TOPIC_STARTER)) {
				String[] paths = topic.split("/");
				Long messageID = Long.valueOf(paths[paths.length - 1]);
				// the last one must contain the message-id
				AbstractRequestObserver requestObserver =
						requestObservers.get(messageID);
				if (requestObserver != null) {
					Response response = new Response(decryptedMesg);
					if (ResponseCode.isSuccess(ResponseCode.valueOf(response.getCode()))) {
						requestObserver.onResponse(response);
					} else {
						requestObserver.onError(response);
					}
				}
				return;
			}

			Request request = new Request(topic, decryptedMesg);
			MQTTExchange exchange = new MQTTExchange(request, null);
			exchange.setMqttClient(this.mqttClient);

			Resource resource = getResource(request);
			if (resource == null) {
				// Check if its a POST operation, in which case
				// we need to return the parent resource to create
				// the new instance
				if (request.getOperation() == Operation.POST) {
					resource = getParentResource(request);
				}
				if (resource == null) {
					exchange.respond(ResponseCode.NOT_FOUND);
					return;
				}
			}
			exchange.setResource(resource);

			switch (request.getOperation()) {
				case POST:
					resource.handlePOST(exchange);
					break;

				case PUT:
					resource.handlePUT(exchange);
					break;

				case DELETE:
					resource.handleDELETE(exchange);
					break;

				case GET:
					resource.handleGET(exchange);
					break;

				case RESET:
					resource.handleRESET(exchange);
					break;
			}
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
		}
	}

	private Resource getResource(Request request) {
		Resource resource = this.mqttClient.getRoot();
		LOG.debug(" root-resource:: "+resource);
		
		
		String id = request.getObjectId();
		LOG.debug(" getObjectId:: "+id);
		if(id == null) {
			return null;
		} else {
			resource = resource.getChild(id);
		}
		
		
		id = request.getObjectInstanceId(); 
		LOG.debug(" getObjectInstanceId:: "+id);
		if(id != null && resource != null) {
			resource = resource.getChild(id);
		}
		
		id = request.getResourceId(); 
		if(id != null && resource != null) {
			resource = resource.getChild(id);
		}
		
		return resource;
	}
	
	private Resource getParentResource(Request request) {
		Resource resource = this.mqttClient.getRoot();
		LOG.debug(" root-resource:: "+resource);
		
		String id = request.getObjectId();
		LOG.debug(" getObjectId:: "+id);
		if(id == null) {
			return null;
		} else {
			resource = resource.getChild(id);
		}
		
		return resource;
	}

	public void addRequest(Long messageID,
			AbstractRequestObserver requestObserver) {
		requestObservers.put(messageID, requestObserver);
	}
	
	public void removeRequest(Long messageID) {
		requestObservers.remove(messageID);
	}
	
	public AbstractRequestObserver getRequest(Long messageID) {
		return requestObservers.get(messageID);
	}
	
	public MQTTWrapper getMqttClinet() {
		// TODO Auto-generated method stub
		return mqttClient;
	}

	public void cancel(long messageID) {
		AbstractRequestObserver obs = this.requestObservers.get(messageID);
		obs.onCancel();
	}

}
