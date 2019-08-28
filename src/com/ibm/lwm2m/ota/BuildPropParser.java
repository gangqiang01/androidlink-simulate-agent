package com.ibm.lwm2m.ota;

import java.io.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildPropParser {
    private static final Logger LOG = LoggerFactory.getLogger(OTAServerManager.class);
    private HashMap<String, String> propHM = null;
    File tmpFile;

    final String TAG = "OTA";

    BuildPropParser(ByteArrayOutputStream out) {
        propHM = new HashMap<String, String>();
        setByteArrayStream(out);
    }

    BuildPropParser(File file) throws IOException {
        propHM = new HashMap<String, String>();
        setFile(file);
    }

    public HashMap<String, String> getPropMap()         { return propHM;};
    public String getProp(String propname) { 
    	if (propHM != null)
    		return (String) propHM.get(propname); 
    	else 
    		return null;
    }

    private void setByteArrayStream(ByteArrayOutputStream out) {
        try {
        	File tmpDir = new File("/tmp/");
        	LOG.debug("tmpDir:"  + tmpDir.toString() +  "\n");
            tmpFile = File.createTempFile("buildprop", "ss", tmpDir);
            
            tmpFile.deleteOnExit();
            FileOutputStream o2 = new FileOutputStream(tmpFile);
            out.writeTo(o2);
            o2.close();
            setFile(tmpFile);
            tmpFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setFile(File file) throws IOException {
        try {
            FileReader reader = new FileReader(file);
            BufferedReader in = new BufferedReader(reader);
            String string;
            while ((string = in.readLine()) != null) {
                Scanner scan = new Scanner(string);
                scan.useDelimiter("=");
                try {
                    propHM.put(scan.next(), scan.next());
                } catch (NoSuchElementException e) {
                    continue;
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public String getRelease() { 
    	if (propHM != null) 
    		return propHM.get("ro.build.version.release"); 
    	else 
    		return null;
    }
    public String getNumRelease()  {
    	if (propHM != null) 
    		return propHM.get("ro.build.version.incremental");
    	else
    		return null;
    }

}
