package com.example.demo.customAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Target is basically what thing it can target like method.
 * 
 * @Retention is the meta annotation(an annotation you put on another annotation). It controls how long the 
 * annotation is retained.
 * 
 * RUNTIME — available at runtime via reflection.
 * 
 * Used @Retention(RetentionPolicy.RUNTIME) so Spring AOP (or any runtime code) can find @Idempotent 
 * annotation while the application runs.
 * 
 * @interface -> that's how java defines custom annotations.
 * 
 * @Target(ElementType.METHOD) — only valid on methods.
 * @Retention(RUNTIME) — visible at runtime via reflection/AOP.
 * 
 * @Retention - These control how long the annotation survives.
 * Without them(meta-annotations), your annotation might be usable in odd places or not available when you need it.
 * 
 * @Retention(RetentionPolicy.SOURCE)
	Meaning:
		The annotation exists only in .java source code.
		The compiler completely removes it when generating .class files.
		It is NOT present in bytecode.
		It is NOT accessible via reflection.
		It is NOT visible to Spring AOP.
		Example usage:
			@Override is SOURCE retention.
			It’s only used by the compiler for validation.
			After compilation → it does not exist anymore.
	
	@Retention(RetentionPolicy.CLASS)
	Meaning:
		The annotation is written into the .class bytecode.
		But the JVM does NOT load it into runtime reflection metadata.
		You cannot access it via reflection.
		Spring cannot see it at runtime.
		This is rarely used unless you’re writing bytecode tools.
		
	@Retention(RetentionPolicy.RUNTIME)
	Meaning:
		Stored in .class
		Loaded by JVM
		Accessible via reflection
		Spring AOP can detect it
		
		Since your aspect relies on:
		
		@Around("@annotation(idempotent)")
		You MUST use RUNTIME. Otherwise Spring won’t see the annotation.


		
		
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
	
	/**
	 * This api() is kinda argument to the annotation, like if annotate a method with the @Idempotent, then 
	 * the compiler forces you to supply api because its required.
	 * 
	 * Why a function/method-like declaration? That’s simply how Java defines annotation attributes — 
	 * each element is defined as a method whose return type is the attribute type, like as here we pass the api
	 * as "ADD_MONEY" so its string right, so the return type will also be string.
	 */
	String api();
}
