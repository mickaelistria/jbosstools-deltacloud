package org.jboss.tools.deltacloud.core;

public class ImageFilter implements IImageFilter {

	private IFieldMatcher nameRule;
	private IFieldMatcher idRule;
	private IFieldMatcher archRule;
	private IFieldMatcher descRule;
	
	@Override
	public boolean isVisible(DeltaCloudImage image) {
		return nameRule.matches(image.getName()) &&
		idRule.matches(image.getId()) &&
		archRule.matches(image.getArchitecture()) &&
		descRule.matches(image.getDescription());
	}

	@Override
	public void setRules(String ruleString) {
		String[] tokens = ruleString.split(";");
		if (tokens[0].equals("*")) {
			nameRule = new AllFieldMatcher();
		} else {
			nameRule = new FieldMatcher(tokens[0]);
		}
		if (tokens[1].equals("*")) {
			idRule = new AllFieldMatcher();
		} else {
			idRule = new FieldMatcher(tokens[1]);
		}
		if (tokens[2].equals("*")) {
			archRule = new AllFieldMatcher();
		} else {
			archRule = new FieldMatcher(tokens[2]);
		}
		if (tokens[3].equals("*")) {
			descRule = new AllFieldMatcher();
		} else {
			descRule = new FieldMatcher(tokens[3]);
		}
	}
	
	@Override
	public String toString() {
		return nameRule + ";" //$NON-NLS-1$ 
		+ idRule + ";"  //$NON-NLS-1$
		+ archRule + ";"  //$NON-NLS-1$
		+ descRule; //$NON-NLS-1$
	}

	public IFieldMatcher getNameRule() {
		return nameRule;
	}
	
	public IFieldMatcher getIdRule() {
		return idRule;
	}
	
	public IFieldMatcher getArchRule() {
		return archRule;
	}
	
	public IFieldMatcher getDescRule() {
		return descRule;
	}
}
