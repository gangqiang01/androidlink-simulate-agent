package com.ibm.lwm2m.ota;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: get the configure from a configure file.
public class OTAServerConfig {
	private static final Logger LOG = LoggerFactory.getLogger(OTAServerConfig.class);
	private String baseUri;
	private String product;
	private String version;

	URL updatePackageURL;
	URL buildpropURL;
	URL configXmlURL;
	URL md5URL;
	
	public OTAServerConfig (String uri, String product, String version) throws MalformedURLException {
		this.baseUri = uri;
		this.product = product;
		this.version = version;
		defaultConfigure(product);
	}
	
	void defaultConfigure(String productname) throws MalformedURLException
	{
		product = productname;
		String fileaddr = new String(baseUri + product + "/" + version + "/" + product + ".ota.zip");
		String md5addr  = new String(baseUri + product + "/" + version + "/" + product + ".ota.zip.md5");
		String buildconfigAddr = new String(baseUri + product + "/" + version +"/" + "build.prop");
		String xmlPath = new String(baseUri + product + "/" + "images.xml");
		updatePackageURL = new URL(fileaddr );
		md5URL = new URL(md5addr);
		buildpropURL = new URL(buildconfigAddr);
		configXmlURL = new URL(xmlPath);

		LOG.info("create a new server config: package url " + updatePackageURL.toString());
		LOG.info("md5 file URL:" + md5URL.toString());
		LOG.info("build.prop URL:" + buildpropURL.toString());
		LOG.info("config xml URL:" + configXmlURL.toString());
	}
	
	public URL getPackageURL () { return updatePackageURL; }
	public URL getBuildPropURL() { return buildpropURL; }
	public URL getConfigXmlURL() { return configXmlURL; }
	public URL getMd5URL() { return md5URL; }
	
}
