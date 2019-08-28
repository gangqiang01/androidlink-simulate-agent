package com.ibm.lwm2m.ota;
import java.net.*;
import java.security.GeneralSecurityException;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.lwm2m.utils.SystemUtil;
import com.ibm.lwm2m.utils.FileUtils;
import com.ibm.lwm2m.utils.CommTrans;

public class OTAServerManager  {
	private static final Logger LOG = LoggerFactory.getLogger(OTAServerManager.class);
	public interface OTAStateChangeListener {
		
		final int STATE_IN_IDLE = 0;
		final int STATE_IN_CHECKED = 1; // state in checking whether new available.
		final int STATE_IN_DOWNLOADING = 2; // state in download upgrade package
		final int STATE_IN_UPGRADING = 3;  // In upgrade state

		final int MESSAGE_DOWNLOAD_PROGRESS = 5;
		final int MESSAGE_VERIFY_PROGRESS = 6;
		final int MESSAGE_STATE_CHANGE = 7;
		final int MESSAGE_ERROR = 8;
		
		// should be raise exception ? but how to do exception in async mode ?
		final int NO_ERROR = 0;
		final int ERROR_WIFI_NOT_AVALIBLE = 1;  // require wifi network, for OTA app.
		final int ERROR_CANNOT_FIND_SERVER = 2;
		final int ERROR_PACKAGE_VERIFY_FAILED = 3;
		final int ERROR_WRITE_FILE_ERROR = 4;
		final int ERROR_NETWORK_ERROR = 5;
		final int ERROR_PACKAGE_INSTALL_FAILED = 6;

		// results
		final int RESULTS_ALREADY_LATEST = 1;

		public void onStateOrProgress(int message, int error, Object info);
		
	}
	private OTAStateChangeListener mListener;	
	private OTAServerConfig mConfig;
	private BuildPropParser parser = null;
	long mCacheProgress = -1;
	boolean mStop = false;
	//modify for locale update   2013/10/28
	String mUpdatePackageLocation = "/tmp/update.zip";
	String statusFile = "/tmp/ota_status";
	int splitNum = 5;
	String TAG = "OTAServerManager";
	int dlTryCnt = 5;
	
	public OTAServerManager(String uri, String product, String version) throws MalformedURLException {
		mConfig = new OTAServerConfig(uri, product, version);
		mUpdatePackageLocation = "/tmp/ota-" + CommTrans.getDeviceId() + "-update.zip";
		statusFile = "/tmp/ota-" + CommTrans.getDeviceId() + "-ota_status";
	}

	public OTAStateChangeListener getmListener() {
		return mListener;
	}

	public void setmListener(OTAStateChangeListener mListener) {
		this.mListener = mListener;
	}
	
	public boolean checkNetworkOnline() {
		return true;
	}

	public boolean checkServer(){
		return checkURLOK(mConfig.getPackageURL());
	}

	public void startCheckingVersion() {

		LOG.info(TAG + " startCheckingVersion");
		if (checkURLOK(mConfig.getBuildPropURL()) == false) {
			if (this.mListener != null) {
				if (this.checkNetworkOnline()) {
					reportCheckingError(OTAStateChangeListener.ERROR_CANNOT_FIND_SERVER);
					LOG.info(TAG + " error: cannot connect to  buildprop url!");
                                } 
				else {
					reportCheckingError(OTAStateChangeListener.ERROR_WIFI_NOT_AVALIBLE);
					LOG.info(TAG + " network is not avalible");
                                }  
			}
			
			return;
		}
		
		parser = getTargetPackagePropertyList(mConfig.getBuildPropURL());
		
		if (parser != null) {
			if (this.mListener != null)
				this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_CHECKED, 
						OTAStateChangeListener.NO_ERROR, parser);
		} else {
			reportCheckingError(OTAStateChangeListener.ERROR_WRITE_FILE_ERROR);
		}
	}

	// return true if needs to upgrade
	public boolean compareLocalVersionToServer() {
		boolean upgrade = true;
		if (parser == null) {
			LOG.info(TAG + " compareLocalVersion Without fetch remote prop list.");
			return false;
		}
		String localNumVersion = SystemUtil.getBspVersion();
		String serverNumVersion = parser.getNumRelease();
		Long remoteBuildUTC = (Long.parseLong(parser.getProp("ro.build.date.utc"))) * 1000;
		// *1000 because Build.java also *1000, align with it.
		LOG.info(TAG + " local Version:" + localNumVersion + " remote Version:" + parser.getNumRelease());
		// compare
		//upgrade = remoteBuildUTC > buildutc;
		// here only check build time, in your case, you may also check build id, etc.

		upgrade = SystemUtil.versionIsNew(serverNumVersion, localNumVersion);

		return upgrade;
	}
	
	void publishDownloadProgress(long total, long downloaded) {
		//Log.v(TAG, "download Progress: total: " + total + "download:" + downloaded);
		Long progress = new Long((downloaded*100)/total);
		if (this.mListener != null && progress.longValue() != mCacheProgress) {
			this.mListener.onStateOrProgress(OTAStateChangeListener.MESSAGE_DOWNLOAD_PROGRESS,
					0, progress);
			mCacheProgress = progress.longValue();
		}
	}
	
	void reportCheckingError(int error) {
		if (this.mListener != null ) {
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_CHECKED, error, null);
			LOG.info(TAG + " ---------state in checked----------- ");
                }
        }
	
	void reportDownloadError(int error) {
		if (this.mListener != null)
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_DOWNLOADING, error, null);
	}
	
	void reportInstallError(int error) {
		if (this.mListener != null) {
			this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_UPGRADING, error, null);
			LOG.info(TAG + " ---------state in upgrading----------- ");
                }   
	}
	
	public long getUpgradePackageSize() {
		if (checkURLOK(mConfig.getPackageURL()) == false) {
			LOG.error(TAG + " getUpgradePckageSize Failed");
			return -1;
		}
		
		URL url = mConfig.getPackageURL();
		URLConnection con;
		try {
			con = url.openConnection();
			return con.getContentLength();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public void onStop() {
		mStop = true;
	}


	private String getRemoteMd5Value(URL md5Url){
        try {
			URLConnection conexion = md5Url.openConnection();
			conexion.setReadTimeout(10000);

            InputStream reader = md5Url.openStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            StringBuilder stringBuilder = new StringBuilder();

            //Log.d(TAG, "start download: " + md5Url.toString() + "to buffer");

            while ((bytesRead = reader.read(buffer)) > 0) {
                stringBuilder.append(new String(buffer, 0, bytesRead));
            }
            reader.close();
            //Log.d(TAG, "reading md5value:" + stringBuilder.toString());
            return stringBuilder.toString().replace("\n", "").replace("\r", "");

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

	public void startDownloadUpgradePackage() {

		LOG.info(TAG + " startDownloadUpgradePackage(), location:" + mUpdatePackageLocation);
		if (checkURLOK(mConfig.getPackageURL()) == false) {
			if (this.mListener != null) {
				reportDownloadError(OTAStateChangeListener.ERROR_CANNOT_FIND_SERVER);
				LOG.info(TAG + " error: cannot connect to package url!");
			}
			return;
		}
		String remoteMd5Value = getRemoteMd5Value(mConfig.getMd5URL());
		if(remoteMd5Value == null){
			if (this.mListener != null) {
				reportDownloadError(OTAStateChangeListener.ERROR_CANNOT_FIND_SERVER);
				LOG.info(TAG + " error: cannot connect to md5file url!");
			}
			return;
		}
		LOG.info(TAG + " remote md5value:" + remoteMd5Value + ", len:" + remoteMd5Value.length());

		for(int i=0; i<dlTryCnt; i++) {
			File targetFile = new File(mUpdatePackageLocation);
			if (targetFile.isFile() && targetFile.exists())
				targetFile.delete();
			try {
				targetFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				reportDownloadError(OTAStateChangeListener.ERROR_WRITE_FILE_ERROR);
				return;
			}

			try {

				URL url = mConfig.getPackageURL();
				LOG.info(TAG + " start downoading package:" + url.toString());
				URLConnection conexion = url.openConnection();
				conexion.setReadTimeout(10000);
				// this will be useful so that you can show a topical 0-100% progress bar

				int lengthOfFile = 96038693;
				lengthOfFile = conexion.getContentLength();
				// download the file
				InputStream input = new BufferedInputStream(url.openStream());
				OutputStream output = new FileOutputStream(targetFile);

				LOG.info(TAG + " file size:" + lengthOfFile);
				byte data[] = new byte[100 * 1024];
				long total = 0, count;
				while ((count = input.read(data)) >= 0 && !mStop) {
					total += count;

					// publishing the progress....
					publishDownloadProgress(lengthOfFile, total);
					output.write(data, 0, (int) count);
				}

				output.flush();
				output.close();
				input.close();
				// check md5 value
				String localMd5Value = SystemUtil.getFileMD5(mUpdatePackageLocation);
				LOG.info(TAG + " try " + i + ": local md5value:" + localMd5Value + ", len:" + localMd5Value.length());
				if(!remoteMd5Value.contains(localMd5Value)){
					if(i < 4) {
						continue;
					} else{
						reportDownloadError(OTAStateChangeListener.ERROR_WRITE_FILE_ERROR);
						break;
					}
				}

				if (this.mListener != null && !mStop) {
					this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_DOWNLOADING, 0, null);
					break;
				}
			} catch (IOException e) {
				e.printStackTrace();
				reportDownloadError(OTAStateChangeListener.ERROR_WRITE_FILE_ERROR);
			} finally {
				;
			}
		}
	}
	
	public void startInstallUpgradePackage() {
		File recoveryFile = new File(mUpdatePackageLocation);
		if(!recoveryFile.exists())  return;
		   
		// first verify package
		LOG.info(TAG + " begin verify");
		LOG.info(TAG + " finish verify");
		FileUtils.write(statusFile, "OK");

		// I must report STATE_IN_UPGRADING success before reboot, there are certain risks to fail
		this.mListener.onStateOrProgress(OTAStateChangeListener.STATE_IN_UPGRADING, 0, null);


		//reportInstallError(OTAStateChangeListener.ERROR_PACKAGE_INSTALL_FAILED);
		LOG.info(TAG + " install finally");
	}

	public void copyToCache(String packageName) {
		try {	
			int bytesum = 0;   
			int byteread = 0;	
			File updatePackage = new File(packageName);	
			if (updatePackage.exists()) { 
				InputStream inStream = new FileInputStream(updatePackage);
				FileOutputStream fs = new FileOutputStream(mUpdatePackageLocation);   
				byte[] buffer = new byte[1444];   
				int length;   
				while ( (byteread = inStream.read(buffer)) != -1) {   
					bytesum += byteread;
					fs.write(buffer, 0, byteread);	 
				}	
				inStream.close();	
			}	
		} catch (Exception e) {
			e.printStackTrace();   
		}	
	}
	
	boolean checkURLOK(URL url) {
		HttpURLConnection con = null;
		try {
			LOG.info("checkURLOK(), url: " + url);
			HttpURLConnection.setFollowRedirects(false);
			con =  (HttpURLConnection) url.openConnection();
			con.setConnectTimeout(10000);
			con.setReadTimeout(10000);
			con.setRequestMethod("HEAD");

			return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}
	
	
	// function: 
	// download the property list from remote site, and parse it to peroerty list.
	// the caller can parser this list and get information.
	BuildPropParser getTargetPackagePropertyList(URL configURL) {
		
		// first try to download the property list file. the build.prop of target image.
		try {
			URL url =  configURL;
			URLConnection conexion = url.openConnection();
			conexion.setReadTimeout(10000);

			InputStream reader = url.openStream();
			ByteArrayOutputStream writer = new ByteArrayOutputStream();
			byte[] buffer = new byte[153600];
			int totalBufRead = 0;
			int bytesRead;

			LOG.info(TAG + " start download: " + url.toString() + "to buffer");
		
			while ((bytesRead = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, bytesRead);
				buffer = new byte[153600];
				totalBufRead += bytesRead;
			}


			LOG.info(TAG + " download finish:" + (new Integer(totalBufRead).toString()) + "bytes download");
		reader.close();
		
		BuildPropParser parser = new BuildPropParser(writer);
		
		return parser;
		
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
