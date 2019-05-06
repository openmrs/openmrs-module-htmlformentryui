package org.openmrs.module.htmlformentryui.constant;

public interface TagsConstant {
	
	public interface TagName {
		
		public static String HTMLFORMENTRY_DATE_PICKER_TAGS = "datepicker";

		public static String HTMLFORMENTRY_INCLUDE_FRAGMENT_TAGS = "includeFragment";
	}
	
	public interface Params {
		
		public static String HTMLFORMENTRY_PARAMS_PROVIDERS = "providers";
		
		public static String HTMLFORMENTRY_PARAMS_FRAGMENTID = "fragmentId";
		
		public static String HTMLFORMENTRY_PARAMS_ADDITIONAL = "params";
		
		public static String HTMLFORMENTRY_PARAMS_PLACEHOLDER = "placeholder";
		
		public static String HTMLFORMENTRY_PARAMS_ID = "id";
		
		public static String HTMLFORMENTRY_PARAMS_CONCEPT = "conceptId";
		
		public static String HTMLFORMENTRY_PARAMS_CLASSES = "classes";
		
	}
	
	public interface ElementLiterals {
		
		public static final String PLACEHOLDER_NAME = "{{NAME}}";
		
		public static final String PARAMETER_GROUPING_CONCEPT_UUID = "groupingConceptUUID";
		
		public static final String PLACEHOLDER_ID = "{{ID}}";
		
		public static String HTMLFORMENTRY_INPUT_TYPE_TEXT = "<input type=text id='{{ID}}' class='{{CLASSES}}' name='{{NAME}}' value='{{INITIALVALUE}}'/>";
		
		public static String HTMLFORMENTRY_SPAN_TYPE_TEXT = "<span id='{{ID}}' class='{{CLASSES}}'>{{INITIALVALUE}}<span/>";
		
		public interface SubstitutePlaceholder {
			
			public static String INITIAL_VALUE = "{{INITIALVALUE}}";
			
			public static String CLASSES = "{{CLASSES}}";
		}
		
		
	}
	
}
