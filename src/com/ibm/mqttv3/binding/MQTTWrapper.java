package com.ibm.mqttv3.binding;

import java.net.InetSocketAddress;

import com.ibm.lwm2m.client.LwM2MClient;
import com.ibm.lwm2m.utils.SystemUtil;
import com.ibm.lwm2m.utils.DESUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.UnsupportedEncodingException;

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(MQTTWrapper.class);
	
	private static int QOS = 2;
	
	// "tcp://localhost:1800";
    private final InetSocketAddress brokerAddress;
    private final String endpointID;
    
    /* root resource */
    private final Resource root;
    
	private IMqttClient mqttClient = null;
	private MqttConnectOptions connOpts = null;
	private MqttCallback callback;
	private String userName;
	private String pwd;

	public MQTTWrapper(InetSocketAddress brokerAddress, String endpointID, String username, String passwd) {
		this.brokerAddress = brokerAddress;
		this.endpointID = endpointID;
		this.root = new RootResource();
		this.userName = username;
		this.pwd = passwd;
	}

	public void reConnect() throws Exception {
		if(null != mqttClient) {
			mqttClient.connect(connOpts);
		}
	}

	public Boolean start(String serverURI, LwM2MClient client, MqttCallback callback) {
		MemoryPersistence persistence = new MemoryPersistence();
		try {
			mqttClient = new MqttClient(serverURI, endpointID, persistence);

			// set option
			LOG.info("username:" + userName);
			LOG.info("password:" + pwd);
			connOpts = new MqttConnectOptions();
			connOpts.setUserName(userName);
			connOpts.setPassword(pwd.toCharArray());
            connOpts.setCleanSession(true);
            //set will message
			String willTopic = client.getWillTopic();
			String willMsg = client.getWillMsg();
			byte  msg[] = DESUtil.encrypt(willMsg).getBytes("UTF-8");
			connOpts.setWill(willTopic, msg, QOS, true);
			connOpts.setAutomaticReconnect(true);
			// set option end

            LOG.info("Connecting endpoint "+ endpointID + " to broker: "+serverURI);
            // set callback before connect, so after connect, the callback functon connectComplete() will be called
			mqttClient.setCallback(callback);
			this.callback = callback;
            mqttClient.connect(connOpts);
            LOG.info("Connected");
		} catch(MqttException me) {
            LOG.error("reason "+me.getReasonCode());
            LOG.error("msg "+me.getMessage());
            LOG.error("loc "+me.getLocalizedMessage());
            LOG.error("cause "+me.getCause());
            LOG.error("excep "+me);
            me.printStackTrace();
            return false;
        } catch(UnsupportedEncodingException e){
		    e.printStackTrace();
		    return false;
        }catch(Exception e){
        	e.printStackTrace();
        	return false;
		}
		return true;
	}
	
	public void stop() {
		try {
            LOG.info("Disconnecting " + endpointID + " from broker");
            mqttClient.disconnect();
		} catch(MqttException me) {
			LOG.info("disconnect error: ");
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
        }
	}
	
	public void setCallBack(MqttCallback callback) {
		mqttClient.setCallback(callback);
		this.callback = callback;
	}
	
	public void subscribe(String topic, int qos) {
		try {
			LOG.info("Subscribe to :: "+ topic);
			mqttClient.subscribe(topic, qos);
		} catch (MqttException me) {
			LOG.info("subscribe error: ");
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
		}
	}
	
	public void subscribe(String[] topics, int[] qos) {
		try {
			for(int i = 0; i < topics.length; i++) {
				LOG.info("Subscribe to :: "+topics[i]);
			}
			mqttClient.subscribe(topics, qos);
		} catch (MqttException me) {
			LOG.info("subscribe error: ");
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
		}
	}
	
	public void publish(String topic, String content) {
		LOG.info("publish :: {"+topic+" ["+content+" ]}");
		String encryptedStr = DESUtil.encrypt(content);
		try {
			MqttMessage message = new MqttMessage(encryptedStr.getBytes("UTF-8"));
            message.setQos(QOS);
            //LOG.info("publish :: {"+topic+" ["+message+" ]}");
			mqttClient.publish(topic, message);
		} catch (MqttException me) {
			LOG.info("public error: {"+topic+" ["+content+" ]}");
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
		} catch (UnsupportedEncodingException e){
			e.printStackTrace();
		}
	}
	
	public MQTTWrapper add(Resource... resources) {
		for (Resource r:resources) {
			LOG.info("adding resource "+ r.getName() +" under root");
			root.add(r);
		}
		return this;
	}

	public Resource getRoot() {
		// TODO Auto-generated method stub
		return root;
	}
	
	public MqttCallback getMqttCallback() {
		return this.callback;
	}
	
	private class RootResource extends MQTTResource {
		
		public RootResource() {
			super("");
		}
		
		@Override
		public void handleGET(MQTTExchange exchange) {
			exchange.respond(ResponseCode.CONTENT, "Hi, Am root resource");
		}
		
	}

	public void destroy() {
		try {
            LOG.info("Disconnecting " + endpointID + " from broker");
            mqttClient.disconnect();
            mqttClient.close();
		} catch(MqttException me) {
			LOG.info("destroy error: ");
			LOG.error("reason "+me.getReasonCode());
			LOG.error("msg "+me.getMessage());
			LOG.error("loc "+me.getLocalizedMessage());
			LOG.error("cause "+me.getCause());
			LOG.error("excep "+me);
            me.printStackTrace();
        }
		mqttClient = null;
	}
}
