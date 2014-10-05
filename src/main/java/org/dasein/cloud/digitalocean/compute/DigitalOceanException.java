package org.dasein.cloud.digitalocean.compute;


import org.dasein.cloud.CloudErrorType;

@SuppressWarnings("serial")
public class DigitalOceanException extends Exception {
	private String         code      = null;
	private CloudErrorType errorType = null;
	private String         requestId = null;
	private int            status    = 0;
	
	public DigitalOceanException(int status, String requestId, String code, String message) {
		super(message);
		this.requestId = requestId;
		this.code = code;
		this.status = status;
		if( code.equals("Throttling") ) {
		    errorType = CloudErrorType.THROTTLING;
		}
		else if( code.equals("TooManyBuckets") ) {
		    errorType = CloudErrorType.QUOTA;
		}
	}
	   
	public String getCode() {
		return code;
	}
	
	public CloudErrorType getErrorType() {
	    return (errorType == null ? CloudErrorType.GENERAL : errorType);
	}
	
	public String getRequestId() {
		return requestId;
	}
	
	public int getStatus() {
		return status;
	}
	
	public String getSummary() { 
		return (status + "/" + requestId + "/" + code + ": " + getMessage());
	}
}
