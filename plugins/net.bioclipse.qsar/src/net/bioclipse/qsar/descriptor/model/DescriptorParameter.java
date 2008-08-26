package net.bioclipse.qsar.descriptor.model;

public class DescriptorParameter {

	String key;
	String defaultvalue;
	String description;
	
	public DescriptorParameter(String key, String defaultValue) {
		this.key=key;
		this.defaultvalue=defaultValue;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getDefaultvalue() {
		return defaultvalue;
	}

	public void setDefaultvalue(String defaultvalue) {
		this.defaultvalue = defaultvalue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	

}