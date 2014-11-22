package de.timmeey.anoBitT.config;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

public class GuiceAnnotations {

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface TorProperties {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface AppProperties {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface InternalHTTPRequestExecutor {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface DHTProperties {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface AnonSocketFactory {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface NonAnonSocketFactory {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface InternalHTTPRequestSocketPool {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface ExternalHTTPRequestSocketPool {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface ExternalHTTPRequestExecutor {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface ExternalHTTPRequestService {
	}

	@BindingAnnotation
	@Target({ FIELD, PARAMETER, METHOD })
	@Retention(RUNTIME)
	public @interface InternalHTTPRequestService {
	}

}
