/*
* Copyright (C) 2014 41concepts Aps
*/
package com.fortyoneconcepts.valjogen.processor.builders;

import java.beans.Introspector;
import java.io.Externalizable;
import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.Diagnostic.Kind;

import com.fortyoneconcepts.valjogen.annotations.types.DataConversion;
import com.fortyoneconcepts.valjogen.annotations.types.Mutability;
import com.fortyoneconcepts.valjogen.model.*;
import com.fortyoneconcepts.valjogen.model.Modifier;
import com.fortyoneconcepts.valjogen.model.NoType;
import com.fortyoneconcepts.valjogen.model.util.*;
import com.fortyoneconcepts.valjogen.processor.DiagnosticMessageConsumer;
import com.fortyoneconcepts.valjogen.processor.ProcessorMessages;
import com.fortyoneconcepts.valjogen.processor.ResourceLoader;
import com.fortyoneconcepts.valjogen.processor.STTemplates;
import com.fortyoneconcepts.valjogen.processor.TemplateKind;

import static com.fortyoneconcepts.valjogen.model.util.NamesUtil.*;
import static com.fortyoneconcepts.valjogen.processor.builders.BuilderUtil.*;

/***
 * This class is responsible for transforming data in the javax.lang.model.* format to our own valjogen models.
 *
 * The javax.lang.model.* lacks detailed documentation so some points is included here:
 * - Elements are about the static structure of the program, ie packages, classes, methods and variables (similar to what is seen in a package explorer in an IDE).
 * - Types are about the statically defined type constraints of the program, i.e. types, generic type parameters, generic type wildcards (Everything that is part of Java's type declarations before type erasure).
 * - Mirror objects is where you can see the reflection of the object, thus seperating queries from the internal structure. This allows reflectiong on stuff that has not been loaded.
 *
 * Nb: Instances of this class is not multi-thread safe. Create a new for each thread.
 *
 * Note that we build our class gradually, so be careful when calling method on class objects while they are constructed. Simple method are generally safe, while methods that depend on structure initialization
 * should not be called unless the class is fully initialized.
 *
 * @author mmc
 */
public final class ModelBuilder
{
	@SuppressWarnings("unused")
	private final static Logger LOGGER = Logger.getLogger(ModelBuilder.class.getName());

	private static final String comparableClass = Comparable.class.getName();
	private static final String serializbleClass = Serializable.class.getName();
	private static final String externalizableClass = Externalizable.class.getName();

	private static final String compareToOverloadName = "compareTo(T)";

	/**
	 * Controls when corresponding available ST template methods should be called.
	 */
	@SuppressWarnings("serial")
	private static final HashMap<String, Predicate4<Configuration, Clazz, List<Method>, List<Member>>> templateMethodConditions = new HashMap<String, Predicate4<Configuration, Clazz, List<Method>, List<Member>>>() {{
		 put("hashCode()", (cfg, clazz, methods, members) -> cfg.isHashEnabled());
		 put("equals(Object)", (cfg, clazz, methods, members) -> cfg.isEqualsEnabled());
		 put("toString()", (cfg, clazz, methods, members) -> cfg.isToStringEnabled());
		 put(compareToOverloadName, (cfg, clazz, methods, members) -> mustImplementComparable(clazz, methods));
		 put("writeExternal(ObjectOutput)", (cfg, clazz, methods, members) -> clazz.isOfType(externalizableClass) && members.stream().anyMatch(m -> m.isMutable()));
		 put("readExternal(ObjectInput)", (cfg, clazz, methods, members) -> clazz.isOfType(externalizableClass) && members.stream().anyMatch(m -> m.isMutable()));
		 put("valueOf()", (cfg, clazz, methods, members) -> false); // called internally in a special way by the templates.
		 put("this()", (cfg, clazz, methods, members) -> false); // called internally in a special way by the templates.
	}};

	private final TypeBuilder typeBuilder;

	private final Types types;
	private final Elements elements;
	private final DiagnosticMessageConsumer errorConsumer;
	private final TypeElement masterInterfaceElement;
	private final Configuration configuration;
	private final ResourceLoader resourceLoader;
	private final STTemplates templates;
	private final NoType noType;

	/**
	 * Contains various data that streams need to manipulate and this needs to be accessed by reference.
	 *
	 * @author mmc
	 */
	private final class StatusHolder
	{
		public boolean encountedSynthesisedMembers = false;
	}

	/**
	 * Executable elements (methods) along with their declared mirror types etc.
	 *
	 * @author mmc
	 */
	private final class ExecutableElementInfo
	{
		public final DeclaredType interfaceDecl;
		public final ExecutableElement executableElement;
		public ExecutableElementInfo optOverriddenBy;
		public Method method;

		public ExecutableElementInfo(DeclaredType interfaceDecl, ExecutableElement executableElement)
		{
			this.interfaceDecl=interfaceDecl;
			this.executableElement=executableElement;
			this.optOverriddenBy=null;
			this.method=null;
		}
	}

	/**
	 * Create an instance of this builder that can build the specified class and all dependencies.
	 *
	 * @param types Types helper from javax.lang.model
	 * @param elements Elements helper from jacax.lang.model
	 * @param errorConsumer Where to send errors.
	 * @param masterInterfaceElement The interface that has been selected for code generation (by an annotation).
	 * @param configuration Descripes the user-selected details about what should be generated (combination of annotation(s) and annotation processor setup).
	 * @param resourceLoader What to call to get resource files
	 * @param templates StringTemplate templates holder used to reflect on what methods are supplied.
	 */
	public ModelBuilder(Types types, Elements elements, DiagnosticMessageConsumer errorConsumer, TypeElement masterInterfaceElement, Configuration configuration, ResourceLoader resourceLoader, STTemplates templates)
	{
      this.types=types;
	  this.elements=elements;
	  this.errorConsumer=errorConsumer;
	  this.masterInterfaceElement=masterInterfaceElement;
	  this.configuration=configuration;
	  this.resourceLoader=resourceLoader;
	  this.templates=templates;
	  this.noType=new NoType();
	  this.typeBuilder=new TypeBuilder(types, elements, errorConsumer, masterInterfaceElement, configuration, noType);
	}

	/**
    * Create a Clazz model instance representing a class to be generated along with all its dependent model instances by inspecting
    * javax.lang.model metadata and the configuration provided by annotation(s) read by annotation processor.
	*
	* @return A initialized Clazz which is a model for what our generated code should look like.
	*
	* @throws Exception if a fatal error has occured.
	*/
	public Clazz buildNewCLazz() throws Exception
	{
		// Step 1 - Create clazz:
		DeclaredType masterInterfaceDecl = (DeclaredType)masterInterfaceElement.asType();

		PackageElement sourcePackageElement = elements.getPackageOf(masterInterfaceElement);

		String sourceInterfacePackageName = sourcePackageElement.isUnnamed() ? "" : sourcePackageElement.getQualifiedName().toString();
		String className = createQualifiedClassName(masterInterfaceElement.asType().toString(), sourceInterfacePackageName);
		String classPackage = NamesUtil.getPackageFromQualifiedName(className);

		String classJavaDoc = elements.getDocComment(masterInterfaceElement);
		if (classJavaDoc==null) // hmmm - seems to be null always (api not working?)
			classJavaDoc="";

		String headerFileName = configuration.getHeaderFileName();
		String headerText = "";
		if (headerFileName!=null)
		{
			headerText=resourceLoader.getResourceAsText(headerFileName);
		}

		Clazz clazz = new Clazz(configuration, className, masterInterfaceElement.getQualifiedName().toString(), classJavaDoc, headerText, (c) -> typeBuilder.createHelperTypes(c));
		noType.init(clazz);

		// Lookup mirror types for base class and interfaces
		DeclaredType baseClazzDeclaredMirrorType = typeBuilder.createBaseClazzDeclaredType(classPackage);
		if (baseClazzDeclaredMirrorType==null)
			return null;

		List<DeclaredType> allBaseClassDeclaredMirrorTypes =  typeBuilder.getSuperTypesWithAscendents(baseClazzDeclaredMirrorType).collect(Collectors.toList());

		String[] ekstraInterfaceNames = configuration.getExtraInterfaces();

		List<DeclaredType> interfaceDeclaredMirrorTypes = typeBuilder.createInterfaceDeclaredTypes(masterInterfaceDecl, ekstraInterfaceNames, classPackage);

		List<DeclaredType> allInterfaceDeclaredMirrorTypes = interfaceDeclaredMirrorTypes.stream().flatMap(ie -> typeBuilder.getDeclaredInterfacesWithAscendents(ie)).collect(Collectors.toList());
		List<DeclaredType> superTypesWithAscendantsMirrorTypes = concat(allInterfaceDeclaredMirrorTypes.stream(), allBaseClassDeclaredMirrorTypes.stream()).distinct().sorted((s1,s2) -> s1.toString().compareTo(s2.toString())).collect(Collectors.toList());

        // Step 2 - Init type part of clzzz:
		List<? extends TypeMirror> typeArgs = masterInterfaceDecl.getTypeArguments();

	    List<Type> typeArgTypes = typeArgs.stream().map(t -> typeBuilder.createType(clazz, t, DetailLevel.Low)).collect(Collectors.toList());

		BasicClazz baseClazzType = (BasicClazz)typeBuilder.createType(clazz, baseClazzDeclaredMirrorType, DetailLevel.High);

		List<Type> interfaceTypes = interfaceDeclaredMirrorTypes.stream().map(ie -> typeBuilder.createType(clazz, ie, DetailLevel.Low)).collect(Collectors.toList());
		Set<Type> superTypesWithAscendants = superTypesWithAscendantsMirrorTypes.stream().map(ie -> typeBuilder.createType(clazz, ie, DetailLevel.Low)).collect(Collectors.toSet());

		clazz.initType(baseClazzType, interfaceTypes, superTypesWithAscendants, typeArgTypes);

		// Step 3 - Init content part of clazz:
		Map<String, Member> membersByName = new LinkedHashMap<String, Member>();
		List<Method> nonPropertyMethods = new ArrayList<Method>();
		List<Property> propertyMethods= new ArrayList<Property>();

		final StatusHolder statusHolder = new StatusHolder();

		// Collect all members, property methods and non-property methods from interfaces paired with the interface they belong to:
		List<ExecutableElementInfo> executableElements = superTypesWithAscendantsMirrorTypes.stream().flatMap(i -> toExecutableElementAndDeclaredTypePair(i, i.asElement().getEnclosedElements().stream().filter(m -> m.getKind()==ElementKind.METHOD).map(m -> (ExecutableElement)m).filter(m -> {
			Set<javax.lang.model.element.Modifier> modifiers = m.getModifiers();
			return !modifiers.contains(javax.lang.model.element.Modifier.PRIVATE);
		}))).collect(Collectors.toList());

		// Note if any methods overrides other methods
		for (ExecutableElementInfo e : executableElements)
		{
			Stream<ExecutableElementInfo> implementationCandidates = executableElements.stream().filter(other -> e.executableElement.getSimpleName().equals(other.executableElement.getSimpleName()));

			implementationCandidates.forEach(cand -> {
				TypeElement interfaceTypeElement = (TypeElement)e.interfaceDecl.asElement();
				boolean overrides = elements.overrides(cand.executableElement, e.executableElement, interfaceTypeElement);
				if (overrides)
					e.optOverriddenBy=cand;
			});
		}

		// Create all Method instances:
		for (ExecutableElementInfo e : executableElements) {
		  Method method=createMethod(clazz, membersByName, statusHolder, e.executableElement, e.optOverriddenBy!=null ? e.optOverriddenBy.executableElement : null, e.interfaceDecl);

		  if (method instanceof Property)
			  propertyMethods.add((Property)method);
		  else nonPropertyMethods.add(method);

		  e.method=method;
		}

		// Second pass at prevously created instances where we setup dependencies between methods.
		for (ExecutableElementInfo e : executableElements) {
			if (e.optOverriddenBy!=null) {
			  e.method.setOverriddenByMethod(e.optOverriddenBy.method);
			}
		}

		if (statusHolder.encountedSynthesisedMembers && configuration.isWarningAboutSynthesisedNamesEnabled())
			errorConsumer.message(masterInterfaceElement, Kind.MANDATORY_WARNING, String.format(ProcessorMessages.ParameterNamesUnavailable, masterInterfaceElement.toString()));

		List<Type> importTypes = createImportTypes(clazz, baseClazzDeclaredMirrorType, interfaceDeclaredMirrorTypes);

		List<Member> members = new ArrayList<Member>(membersByName.values());

		if (clazz.isOfType(serializbleClass)) {
			nonPropertyMethods.addAll(createMagicSerializationMethods(clazz));
		}

		Set<String> applicableTemplateImplementedMethodNames = templates.getAllTemplateMethodNames().stream().filter(n -> {
			Predicate4<Configuration, Clazz, List<Method>, List<Member>> predicate = templateMethodConditions.get(n);
			return predicate!=null ? predicate.test(clazz.getConfiguration(), clazz, nonPropertyMethods, members) : true;
		}).collect(Collectors.toSet());

		claimAndVerifyMethods(nonPropertyMethods, propertyMethods, applicableTemplateImplementedMethodNames);

		Map<String, Member> baseMembersByName = baseClazzType.getMembers().stream().collect(Collectors.toMap(m -> m.getName(), m -> m));

		boolean implementsComparable = mustImplementComparable(clazz, nonPropertyMethods);
		Optional<Method> comparableMethodToImplement = implementsComparable ? nonPropertyMethods.stream().filter(m -> m.getOverloadName().equals(compareToOverloadName) && m.getDeclaredModifiers().contains(Modifier.ABSTRACT)).findFirst() : Optional.empty();
		List<Member> selectedComparableMembers = implementsComparable ? getSelectedComparableMembers(membersByName, baseMembersByName, comparableMethodToImplement.orElse(null)) : Collections.emptyList();

		EnumSet<Modifier> classModifiers = configuration.getClazzModifiers();
		boolean isAbstractClazz = nonPropertyMethods.stream().anyMatch(m -> m.getImplementationInfo()==ImplementationInfo.IMPLEMENTATION_MISSING) || (classModifiers!=null && classModifiers.contains(Modifier.ABSTRACT));
        if (classModifiers==null) {
        	if (isAbstractClazz)
    			classModifiers=EnumSet.of(Modifier.PUBLIC, Modifier.ABSTRACT);
    		else
    			classModifiers=EnumSet.of(Modifier.PUBLIC, Modifier.FINAL);
        }

	    nonPropertyMethods.addAll(createConstructorsAndFactoryMethods(clazz, baseClazzType, members, classModifiers, configuration.getBaseClazzConstructors()));

	    List<Annotation> clazzAnnotations = Arrays.stream(configuration.getClazzAnnotations()).map(s -> new Annotation(clazz, s)).collect(Collectors.toList());

		clazz.initContent(members, propertyMethods, nonPropertyMethods, filterImportTypes(clazz, importTypes), selectedComparableMembers, classModifiers, clazzAnnotations);

		return clazz;
	}

	private void claimAndVerifyMethods(List<Method> nonPropertyMethods, List<Property> propertyMethods, Set<String> applicableTemplateImplementedMethodNames)
	{
		Set<String> unusedMethodNames = new HashSet<String>(applicableTemplateImplementedMethodNames);

		for (Method method : nonPropertyMethods)
		{
			if (!method.isOverridden()) {
				String name = method.getOverloadName();
				for (String templateMethodName : applicableTemplateImplementedMethodNames)
				{
					if (name.equals(templateMethodName)) {
						unusedMethodNames.remove(templateMethodName);

						method.setImplementationInfo(ImplementationInfo.IMPLEMENTATION_PROVIDED_BY_THIS_OBJECT);
						break;
					}
				}
			}
		}

		for (String name : unusedMethodNames) {
		    errorConsumer.message(masterInterfaceElement, Kind.ERROR, String.format(ProcessorMessages.UNKNOWN_METHOD, name));
		}

		for (Method method : propertyMethods)
		{
			method.setImplementationInfo(ImplementationInfo.IMPLEMENTATION_PROVIDED_BY_THIS_OBJECT);
		}
	}

	private List<Method> createConstructorsAndFactoryMethods(Clazz clazz, ObjectType baseClazzType, List<Member> members, EnumSet<Modifier> classModifiers, String[] baseClazzConstructors)
	{
		List<Constructor> baseClassConstructors = baseClazzType.getConstructors();

		List<Method> result = new ArrayList<>();

		boolean includeFactoryMethod = !classModifiers.contains(Modifier.ABSTRACT) && configuration.isStaticFactoryMethodEnabled();
		boolean mustHaveDefaultPublicCtr = (clazz.isOfType(Externalizable.class));

		EnumSet<Modifier> factoryModifiers = EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);

		// Find the first of the largest base constructor measured int paramter count.
		Constructor largestBaseClassConstructor = null;
		for (Constructor baseClassConstructor : baseClassConstructors)
		{
			if (largestBaseClassConstructor==null || baseClassConstructor.getParameters().size()>largestBaseClassConstructor.getParameters().size())
				largestBaseClassConstructor=baseClassConstructor;
		}

		boolean hasDefaultCtr = false;

		// Add our own constructors that delegate to base constructors.
		for (Constructor baseClassConstructor : baseClassConstructors)
		{
			String baseClassConstructorOverLoadName = baseClassConstructor.getOverloadName();
			boolean enabled = Arrays.stream(baseClazzConstructors).anyMatch(b -> matchingOverloads(baseClassConstructorOverLoadName, b, true));
			boolean primaryBase = (baseClassConstructor==largestBaseClassConstructor); // The base constructor with the most arguments ?

			if (enabled) {
				List<List<Parameter>> classConstructorsParameterLists = createConstuctorsParameterLists(clazz, members, primaryBase, includeFactoryMethod);
				for (List<Parameter> classParameters : classConstructorsParameterLists) {
					boolean primary = primaryBase && classParameters==classConstructorsParameterLists.get(0); // First one for primary base constructor is the one with most arguments.

					Stream<Parameter> baseClassParameters = baseClassConstructor.getParameters().stream().map(p -> new DelegateParameter(clazz, p.getType().copy(clazz), p.getName(), p.getDeclaredModifiers(), createConstructorParameterAnnotations(clazz, true, p.getName(), includeFactoryMethod), baseClassConstructor, p));
					List<Parameter> parameters = concat(baseClassParameters, classParameters.stream()).collect(Collectors.toList());

					EnumSet<Modifier> constructorModifiers = createConstructorModifiers(classModifiers, parameters, includeFactoryMethod, mustHaveDefaultPublicCtr);

					DelegateConstructor constructor = new DelegateConstructor(clazz, clazz, noType, parameters, baseClassConstructor.getThrownTypes(), "", primary, EnumSet.noneOf(Modifier.class), constructorModifiers, createConstructorAnnotations(clazz, parameters, primary, includeFactoryMethod), ImplementationInfo.IMPLEMENTATION_PROVIDED_BY_THIS_OBJECT, baseClassConstructor);
					result.add(constructor);
					hasDefaultCtr|=(parameters.size()==0);
				}

				if (includeFactoryMethod) {
					List<List<Parameter>> classFactoryMethodParameterLists = createFactoryMethodsParameterLists(clazz, members, primaryBase);
					for (List<Parameter> classParameters : classFactoryMethodParameterLists) {
						boolean primary = primaryBase && classParameters==classFactoryMethodParameterLists.get(0); // First one for primary base constructor is the one with most arguments.

						Stream<Parameter> baseClassParameters = baseClassConstructor.getParameters().stream().map(p -> new Parameter(clazz, p.getType().copy(clazz), p.getName(), p.getDeclaredModifiers(), createFactoryMethodParameterAnnotations(clazz, primary, p.getName())));
						List<Parameter> parameters = concat(baseClassParameters, classParameters.stream()).collect(Collectors.toList());

						Method factoryMethod = new FactoryMethod(clazz, clazz, ConfigurationDefaults.factoryMethodName, clazz, parameters, baseClassConstructor.getThrownTypes(), "", primary, EnumSet.noneOf(Modifier.class), factoryModifiers, createFactoryMethodAnnotations(clazz, parameters, primary), ImplementationInfo.IMPLEMENTATION_PROVIDED_BY_THIS_OBJECT, TemplateKind.UNTYPED);
						result.add(factoryMethod);
					}
				}
			}
		}

		// Just add our own if we did not find any base constructors.
		if (result.isEmpty()) {
			List<List<Parameter>> classConstructorsParameterLists = createConstuctorsParameterLists(clazz, members, true, includeFactoryMethod);
			for (List<Parameter> parameters : classConstructorsParameterLists) {
				boolean primary = (parameters==classConstructorsParameterLists.get(0)); // First one is the one with most arguments.

				EnumSet<Modifier> constructorModifiers = createConstructorModifiers(classModifiers, parameters, includeFactoryMethod, mustHaveDefaultPublicCtr);

				Constructor constructor = new Constructor(clazz, clazz, noType, parameters, Collections.emptyList(), "", primary, EnumSet.noneOf(Modifier.class), constructorModifiers, createConstructorAnnotations(clazz, parameters, primary, includeFactoryMethod), ImplementationInfo.IMPLEMENTATION_PROVIDED_BY_THIS_OBJECT);
				result.add(constructor);
				hasDefaultCtr|=(parameters.size()==0);
			}

			if (includeFactoryMethod) {
				List<List<Parameter>> classFactoryMethodParameterLists = createFactoryMethodsParameterLists(clazz, members, true);
				for (List<Parameter> parameters : classFactoryMethodParameterLists) {
					boolean primary = (parameters==classFactoryMethodParameterLists.get(0)); // First one is the one with most arguments.

					Method factoryMethod = new FactoryMethod(clazz, clazz, ConfigurationDefaults.factoryMethodName, clazz, parameters, Collections.emptyList(), "", primary, EnumSet.noneOf(Modifier.class), factoryModifiers, createFactoryMethodAnnotations(clazz, parameters, primary), ImplementationInfo.IMPLEMENTATION_PROVIDED_BY_THIS_OBJECT, TemplateKind.UNTYPED);
					result.add(factoryMethod);
				}
			}
		}

		// Add public default ctr if needed and we have not added it already.
		if (mustHaveDefaultPublicCtr && !hasDefaultCtr)
		{
			boolean primary = false;
			List<Parameter> parameters = Collections.emptyList();
			EnumSet<Modifier> constructorModifiers = createConstructorModifiers(classModifiers, parameters, includeFactoryMethod, mustHaveDefaultPublicCtr);

			Constructor constructor = new Constructor(clazz, clazz, noType, parameters, Collections.emptyList(), "", primary, EnumSet.noneOf(Modifier.class), constructorModifiers, createConstructorAnnotations(clazz, parameters, primary, includeFactoryMethod), ImplementationInfo.IMPLEMENTATION_PROVIDED_BY_THIS_OBJECT);
			result.add(constructor);
		}

		return result;

	}

	private EnumSet<Modifier> createConstructorModifiers(EnumSet<Modifier> classModifiers, List<Parameter> parameters, boolean includeFactoryMethod, boolean mustHaveDefaultPublicCtr)
	{
		EnumSet<Modifier> constructorModifiers;

		if (!includeFactoryMethod || (parameters.size()==0 && mustHaveDefaultPublicCtr)) {
			constructorModifiers=EnumSet.of(Modifier.PUBLIC);
		} else {
			if (classModifiers.contains(Modifier.FINAL))
				constructorModifiers=EnumSet.of(Modifier.PRIVATE);
			else constructorModifiers=EnumSet.of(Modifier.PROTECTED);
		}

		return constructorModifiers;
	}

	private List<List<Parameter>> createConstuctorsParameterLists(Clazz clazz, List<Member> members, boolean primary, boolean includeFactoryMethod)
	{
		List<List<Parameter>> result = new ArrayList<List<Parameter>>();

		// Potential primary constructor (the one with most parameters) added first!
		result.add(members.stream().map(m -> new MemberParameter(clazz, m.getType(), m.getName(), EnumSet.noneOf(Modifier.class), createConstructorParameterAnnotations(clazz, primary, m.getName(), includeFactoryMethod), m)).collect(Collectors.toList()));

		// Members that can be set with setters do not need to be set in constructor, so add one without these. If all members are mutable this will provide a default constructor.
		boolean mutable = members.stream().anyMatch(m -> m.isMutable());
		if (mutable) {
			result.add(members.stream().filter(m -> !m.isMutable()).map(m -> new MemberParameter(clazz, m.getType(), m.getName(), EnumSet.noneOf(Modifier.class), createConstructorParameterAnnotations(clazz, false, m.getName(), includeFactoryMethod), m)).collect(Collectors.toList()));
		}

		return result;
	}

	private List<List<Parameter>> createFactoryMethodsParameterLists(Clazz clazz, List<Member> members, boolean primary)
	{
		List<List<Parameter>> result = new ArrayList<List<Parameter>>();

		// Potential primary factory method (the one with most parameters) added first!
		result.add(members.stream().map(m -> new Parameter(clazz, m.getType(), m.getName(), EnumSet.noneOf(Modifier.class), createFactoryMethodParameterAnnotations(clazz, primary, m.getName()))).collect(Collectors.toList()));

		// Members that can be set with setters do not need to be set in factory method, so add one without these. If all members are mutable this will provide a no-arg factory method.
		boolean mutable = members.stream().anyMatch(m -> m.isMutable());
		if (mutable) {
			result.add(members.stream().filter(m -> !m.isMutable()).map(m -> new Parameter(clazz, m.getType(), m.getName(), EnumSet.noneOf(Modifier.class), createFactoryMethodParameterAnnotations(clazz, false, m.getName()))).collect(Collectors.toList()));
		}

		return result;
	}

	private List<Annotation> createConstructorAnnotations(Clazz clazz, List<Parameter> parameters, boolean primaryConstructor, boolean includeFactoryMethod)
	{
		String overloadName = Method.getOverloadName("", parameters);
 	    List<Annotation> configuredConstructorAnnotations = configuration.getMethodAnnotations(m -> matchingOverloads(m, overloadName, true)).stream().map(pair -> new Annotation(clazz, pair.getValue())).collect(Collectors.toList());

		List<Annotation> result;
		if (primaryConstructor && (configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS || configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS_WITH_JDK8_PARAMETER_NAMES) && !includeFactoryMethod)
		{
			result=new ArrayList<Annotation>(configuredConstructorAnnotations);
			result.add(new Annotation(clazz, "@JsonCreator"));
		} else result=configuredConstructorAnnotations;

		return result;
	}

	private List<Annotation> createFactoryMethodAnnotations(Clazz clazz, List<Parameter> parameters, boolean primaryFactoryMethod)
	{
		String overloadName = Method.getOverloadName(ConfigurationDefaults.factoryMethodName, parameters);
		List<Annotation> configuredFactoryAnnotations = configuration.getMethodAnnotations(m -> matchingOverloads(m, overloadName, true)).stream().map(pair -> new Annotation(clazz, pair.getValue())).collect(Collectors.toList());

		List<Annotation> result;
		if (primaryFactoryMethod && (configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS || configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS_WITH_JDK8_PARAMETER_NAMES))
		{
			result=new ArrayList<Annotation>(configuredFactoryAnnotations);
			result.add(new Annotation(clazz, "@JsonCreator"));
		} else result=configuredFactoryAnnotations;

		return result;
	}

	private List<Annotation> createConstructorParameterAnnotations(Clazz clazz, boolean primaryConstructor, String parameterName, boolean includeFactoryMethod)
	{
		List<Annotation> result = new ArrayList<Annotation>();

		if (primaryConstructor && configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS && !includeFactoryMethod)
		{
			result.add(new Annotation(clazz, "@JsonProperty(\""+parameterName+"\")"));
		}

		return result;
	}

	private List<Annotation> createFactoryMethodParameterAnnotations(Clazz clazz, boolean primaryFactoryMethod, String parameterName)
	{
		List<Annotation> result = new ArrayList<Annotation>();

		if (primaryFactoryMethod && configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS)
		{
			result.add(new Annotation(clazz, "@JsonProperty(\""+parameterName+"\")"));
		}

		return result;
	}

	private List<Method> createMagicSerializationMethods(BasicClazz clazz)
	{
		List<Method> newMethods = new ArrayList<>();

		Type noType = clazz.getHelperTypes().getNoType();

		TypeMirror ioExceptionMirrorType = typeBuilder.createTypeFromString("java.io.IOException");
		TypeMirror objectStreamExceptionMirrorType = typeBuilder.createTypeFromString("java.io.ObjectStreamException");
		TypeMirror classNotFoundExceptionMirrorType = typeBuilder.createTypeFromString("java.lang.ClassNotFoundException");

		ObjectType inputStreamType = clazz.getHelperTypes().getInputStreamType();
		ObjectType objectOutputStreamType = clazz.getHelperTypes().getOutputStreamType();

		EnumSet<Modifier> declaredMethodModifiers=EnumSet.of(Modifier.PRIVATE);
		EnumSet<Modifier> methodModifiers;
		if (configuration.isFinalMethodsEnabled())
			methodModifiers = EnumSet.of(Modifier.PRIVATE, Modifier.FINAL);
		else methodModifiers = EnumSet.of(Modifier.PRIVATE);

		EnumSet<Modifier> declaredParamModifiers = EnumSet.noneOf(Modifier.class);

		List<Annotation> paramterAnnotations = Collections.emptyList();

		// Add : private Object readResolve() throws ObjectStreamException :
		Method readResolve = new Method(clazz, noType, "readResolve", clazz.getHelperTypes().getJavaLangObjectType(), Collections.emptyList(), Collections.singletonList((ObjectType)typeBuilder.createType(clazz, objectStreamExceptionMirrorType, DetailLevel.Low)), "", declaredMethodModifiers, methodModifiers, paramterAnnotations, ImplementationInfo.IMPLEMENTATION_MAGIC, TemplateKind.TYPED);
		newMethods.add(readResolve);

		// Add private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException :
		List<Parameter> readObjectParameters = Collections.singletonList(new Parameter(clazz, inputStreamType, inputStreamType, "in", declaredParamModifiers, paramterAnnotations));
		Method readObject = new Method(clazz, noType, "readObject", clazz.getHelperTypes().getVoidType(), readObjectParameters, Arrays.asList(new ObjectType[] { (ObjectType)typeBuilder.createType(clazz, ioExceptionMirrorType, DetailLevel.Low), (ObjectType)typeBuilder.createType(clazz, classNotFoundExceptionMirrorType, DetailLevel.Low) }), "", declaredMethodModifiers, methodModifiers, paramterAnnotations, ImplementationInfo.IMPLEMENTATION_MAGIC, TemplateKind.TYPED);
		newMethods.add(readObject);

		// Add private void readObjectNoData() throws InvalidObjectException
		Method readObjectNoData = new Method(clazz, noType, "readObjectNoData", clazz.getHelperTypes().getVoidType(), Collections.emptyList(), Collections.singletonList((ObjectType)typeBuilder.createType(clazz, objectStreamExceptionMirrorType, DetailLevel.Low)), "", declaredMethodModifiers, methodModifiers, paramterAnnotations, ImplementationInfo.IMPLEMENTATION_MAGIC, TemplateKind.TYPED);
		newMethods.add(readObjectNoData);

		// Add : private void writeObject (ObjectOutputStream out) throws IOException :
		List<Parameter> writeObjectParameters = Collections.singletonList(new Parameter(clazz, objectOutputStreamType, objectOutputStreamType, "out", declaredParamModifiers, paramterAnnotations));
		Method writeObject = new Method(clazz, noType, "writeObject", clazz.getHelperTypes().getVoidType(), writeObjectParameters, Collections.singletonList((ObjectType)typeBuilder.createType(clazz, ioExceptionMirrorType, DetailLevel.Low)), "", declaredMethodModifiers, methodModifiers, paramterAnnotations, ImplementationInfo.IMPLEMENTATION_MAGIC, TemplateKind.TYPED);
		newMethods.add(writeObject);

		// Add : private Object writeReplace() throws ObjectStreamException :
		Method writeReplace = new Method(clazz, noType, "writeReplace", clazz.getHelperTypes().getJavaLangObjectType(), Collections.emptyList(), Collections.singletonList((ObjectType)typeBuilder.createType(clazz, objectStreamExceptionMirrorType, DetailLevel.Low)), "", declaredMethodModifiers, methodModifiers, paramterAnnotations, ImplementationInfo.IMPLEMENTATION_MAGIC, TemplateKind.TYPED);
		newMethods.add(writeReplace);

		return newMethods;
	}

	private List<Member> getSelectedComparableMembers(Map<String, Member> membersByName, Map<String, Member> baseMembersByName, Method comparableMethodToImplement)
	{
		// TODO: Check if type of comparable argument suits our purpose and give warning/error otherwise if members are not accessible for type.
		// Type comparableArgType = comparableMethodToImplement.getParameters().get(0).getType();

		List<Member> comparableMembers;

		String[] comparableMemberNames = configuration.getComparableMembers();
		if (comparableMemberNames.length==0)
		{
			comparableMembers=membersByName.values().stream().filter(m -> m.getType().isPrimitive() || m.getType().isOfType(comparableClass)).collect(Collectors.toList());
			if (comparableMembers.size()!=membersByName.size())
			{
				errorConsumer.message(masterInterfaceElement, Kind.MANDATORY_WARNING, String.format(ProcessorMessages.NotAllMembersAreComparable, masterInterfaceElement.toString()));
			}
		} else {
			comparableMembers=new ArrayList<Member>();

			for (String comparableMemberName : comparableMemberNames)
			{
				Member member = membersByName.get(comparableMemberName);
				if (member==null)
					member=baseMembersByName.get(comparableMemberName);

				if (member!=null) {
					comparableMembers.add(member);
					if (!member.getType().isPrimitive() && !member.getType().isOfType(comparableClass)) {
						errorConsumer.message(masterInterfaceElement, Kind.ERROR, String.format(ProcessorMessages.MemberNotComparable, comparableMemberName));
					}
				} else {
					errorConsumer.message(masterInterfaceElement, Kind.ERROR, String.format(ProcessorMessages.MemberNotFound, comparableMemberName));
				}
			}
		}

		return comparableMembers;
	}

	private Stream<ExecutableElementInfo> toExecutableElementAndDeclaredTypePair(DeclaredType interfaceOrClasMirrorType, Stream<ExecutableElement> elements)
	{
		return elements.map(e -> new ExecutableElementInfo(interfaceOrClasMirrorType, e));
	}

	private Method createMethod(BasicClazz clazz, Map<String, Member> membersByName, final StatusHolder statusHolder, ExecutableElement m, ExecutableElement mOverriddenBy, DeclaredType interfaceOrClassMirrorType)
	{
	    Method newMethod = null;

		String javaDoc = elements.getDocComment(m);

		if (javaDoc==null) // hmmm - seems to be null always (api not working?)
			javaDoc="";

		ExecutableType executableMethodMirrorType;

		try {
		  executableMethodMirrorType = (ExecutableType)types.asMemberOf(interfaceOrClassMirrorType, m);
		} catch (IllegalArgumentException e) // Workaround for eclipse not liking asMemberOf for generic protypical types (Bug 382590)
		{
			// Eclipse not liking asMemberOf on subtypes (Bug 382590)
			// Lets hope eclipse will fix it otherwise consider doing a workaround with interfaceOrClassMirrorType.getTypeArguments().
	       errorConsumer.message(masterInterfaceElement, Kind.ERROR, "Ran into eclipse bug 382590 - Could not generate correct code for generic subclassing due to this eclipse bug 'https://bugs.eclipse.org/bugs/show_bug.cgi?id=382590'. Please vote on it.");
	       executableMethodMirrorType = (ExecutableType)m.asType(); // Fallback - will produce incorrect code.
		}

		Type declaringType = typeBuilder.createType(clazz, interfaceOrClassMirrorType, DetailLevel.Low);

		String methodName = m.getSimpleName().toString();

		TypeMirror returnTypeMirror = executableMethodMirrorType.getReturnType();
		Type returnType = typeBuilder.createType(clazz, returnTypeMirror, DetailLevel.High);

		List<? extends VariableElement> params =  m.getParameters();
		List<? extends TypeMirror> paramTypes = executableMethodMirrorType.getParameterTypes();

		if (params.size()!=paramTypes.size())
			throw new RuntimeException("Internal error - Numbers of method parameters "+params.size()+" and method parameter types "+paramTypes.size()+" does not match");

		List<? extends TypeMirror> thrownTypeMirrors = executableMethodMirrorType.getThrownTypes();
		List<Type> thrownTypes = thrownTypeMirrors.stream().map(ie -> typeBuilder.createType(clazz, ie, DetailLevel.Low)).collect(Collectors.toList());

		List<Parameter> parameters = new ArrayList<Parameter>();
		for (int i=0; i<params.size(); ++i)
		{
			Parameter param = typeBuilder.createParameter(clazz, params.get(i), paramTypes.get(i), DetailLevel.High);
			parameters.add(param);
		}

		EnumSet<Modifier> declaredModifiers = typeBuilder.createModifierSet(m.getModifiers());

		boolean validProperty = false;
	    if (!m.isDefault() && m.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT))
		{
			PropertyKind propertyKind = null;
		    if (NamesUtil.isGetterMethod(methodName, configuration.getGetterPrefixes()))
		    	propertyKind=PropertyKind.GETTER;
		    else if (NamesUtil.isSetterMethod(methodName, configuration.getSetterPrefixes())) {
		    	String returnTypeName = returnTypeMirror.toString();
				if (returnTypeName.equals("void"))
				  propertyKind=PropertyKind.MUTABLE_SETTER;
			    else propertyKind=PropertyKind.IMMUTABLE_SETTER;
		    }

			if (propertyKind!=null) {
				Member propertyMember = createPropertyMemberIfValidProperty(clazz, interfaceOrClassMirrorType, returnTypeMirror, params, paramTypes, m, propertyKind);

				if (propertyMember!=null) {
		            final Member existingMember = membersByName.putIfAbsent(propertyMember.getName(), propertyMember);
		          	if (existingMember!=null) {
		          	   if (!existingMember.getType().equals(propertyMember.getType())) {
		          		 if (!configuration.isMalformedPropertiesIgnored()) {
		          			  String propertyNames = concat(existingMember.getPropertyMethods().stream().map(p -> p.getName()), of(propertyMember.getName())).collect(Collectors.joining(", "));
		     				  errorConsumer.message(m, Kind.ERROR, String.format(ProcessorMessages.InconsistentProperty, propertyNames ));
		          		 }
		          	   }

		          	   propertyMember = existingMember;
		          	}

		          	Property property = createValidatedProperty(clazz, statusHolder, declaringType, m, returnType, parameters, thrownTypes, javaDoc, propertyKind, propertyMember, declaredModifiers, ImplementationInfo.IMPLEMENTATION_MISSING);

		          	propertyMember.addPropertyMethod(property);
		          	validProperty=true;
		          	newMethod=property;
				}
			}
		}

		if (!validProperty)
		{
		    ImplementationInfo implementationInfo;
		    if (m.isDefault())
				implementationInfo=ImplementationInfo.IMPLEMENTATION_DEFAULT_PROVIDED;
		    else if (!m.getModifiers().contains(javax.lang.model.element.Modifier.ABSTRACT) || mOverriddenBy!=null)
				implementationInfo=ImplementationInfo.IMPLEMENTATION_PROVIDED_BY_BASE_OBJECT;
			else implementationInfo=ImplementationInfo.IMPLEMENTATION_MISSING;

			List<Annotation> methodAnnotations = createMethodAnnotations(clazz, methodName, parameters, declaredModifiers);

		    if (isConstructor(methodName)) {
		    	newMethod=new Constructor(clazz, declaringType, returnType, parameters, thrownTypes, javaDoc, false, declaredModifiers, methodAnnotations, implementationInfo);
		    } else {
		    	newMethod = new Method(clazz, declaringType, methodName, returnType, parameters, thrownTypes, javaDoc, declaredModifiers, methodAnnotations, implementationInfo, TemplateKind.TYPED);
		    }
		}

		return newMethod;
	}

	private List<Annotation> createMethodAnnotations(BasicClazz clazz, String methodName, List<Parameter> parameters, EnumSet<Modifier> modifiers)
	{
		String overloadName = Method.getOverloadName(methodName, parameters);
		List<Annotation> configuredFactoryAnnotations = configuration.getMethodAnnotations(m -> matchingOverloads(m, overloadName, true)).stream().map(pair -> new Annotation(clazz, pair.getValue())).collect(Collectors.toList());

		List<Annotation> result = new ArrayList<Annotation>(configuredFactoryAnnotations);

		if (!isConstructor(methodName) && !modifiers.contains(Modifier.STATIC) && result.stream().noneMatch(a -> a.getCode().contains("@Override")) ) {
			result.add(new Annotation(clazz, "@Override"));
		}

		return result;
	}

	private List<Type> createImportTypes(BasicClazz clazz, DeclaredType baseClazzDeclaredType, List<DeclaredType> implementedDecalredInterfaceTypes)
	{
		List<Type> importTypes = new ArrayList<Type>();
		for (DeclaredType implementedInterfaceDeclaredType : implementedDecalredInterfaceTypes)
		  importTypes.add(typeBuilder.createType(clazz, implementedInterfaceDeclaredType, DetailLevel.Low));

		importTypes.add(typeBuilder.createType(clazz, baseClazzDeclaredType, DetailLevel.Low));

		HelperTypes helperTypes = clazz.getHelperTypes();
		if (configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS || configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS_WITH_JDK8_PARAMETER_NAMES)
		{
			importTypes.add(helperTypes.getJsonCreator());
		}

		if (configuration.getDataConversion()==DataConversion.JACKSON_DATABIND_ANNOTATIONS) {
				importTypes.add(helperTypes.getJsonProperty());
		}

		for (String importName : configuration.getImportClasses())
		{
			TypeElement importElement = elements.getTypeElement(importName);
			if (importElement==null) {
				errorConsumer.message(masterInterfaceElement, Kind.ERROR, String.format(ProcessorMessages.ImportTypeNotFound, importName));
			} else {
			   Type importElementType = typeBuilder.createType(clazz, importElement.asType(), DetailLevel.Low);
			   importTypes.add(importElementType);
			}
		}

		return importTypes;
	}

	private Property createValidatedProperty(BasicClazz clazz, StatusHolder statusHolder, Type declaringType, ExecutableElement m, Type returnType, List<Parameter> parameters, List<Type> thrownTypes, String javaDoc, PropertyKind propertyKind, Member propertyMember, EnumSet<Modifier> modifiers, ImplementationInfo implementationInfo)
	{
		Property property;

		String propertyName = m.getSimpleName().toString();

      	Type overriddenReturnType = (configuration.isThisAsImmutableSetterReturnTypeEnabled() && propertyKind==PropertyKind.IMMUTABLE_SETTER) ? clazz : returnType;

		List<Annotation> methodAnnotations = createMethodAnnotations(clazz, propertyName, parameters, modifiers);

		if (parameters.size()==0) {
			property=new Property(clazz, declaringType, propertyName, returnType, overriddenReturnType, thrownTypes, propertyMember, propertyKind, javaDoc, modifiers, methodAnnotations, implementationInfo);
		} else if (parameters.size()==1) {
			Parameter parameter = parameters.get(0);

			// Parameter names may be syntesised so we can fall back on using member
			// name as parameter name. Note if this happen so we can issue a warning later.
			if (parameter.getName().matches("arg\\d+")) { // Synthesised check (not bullet-proof):
				statusHolder.encountedSynthesisedMembers=true;
				parameter=parameter.setName(propertyMember.getName());
			}

			property = new Property(clazz, declaringType, propertyName, returnType, overriddenReturnType, thrownTypes, propertyMember, propertyKind, javaDoc, modifiers, methodAnnotations, implementationInfo, parameter);
		} else throw new RuntimeException("Unexpected number of formal parameters for property "+m.toString()); // Should not happen for a valid propety unless validation above has a programming error.

		return property;
	}

	private List<Type> filterImportTypes(BasicClazz clazz, List<Type> importTypes)
	{
		List<Type> result = new ArrayList<Type>();

		for (Type type : importTypes)
		{
			if (type.getPackageName().equals("java.lang"))
				continue;

			if (type.getPackageName().equals(clazz.getPackageName()))
			    continue;

			if (result.stream().anyMatch(existingType -> existingType.getQualifiedName().equals(type.getQualifiedName())))
			   continue;

			result.add(type);
		}

		return result;
	}

	private String createQualifiedClassName(String qualifedInterfaceName, String sourcePackageName)
	{
		String className = configuration.getName();
		if (className==null || className.isEmpty())
			className = NamesUtil.createNewClassNameFromInterfaceName(qualifedInterfaceName);

		if (!NamesUtil.isQualifiedName(className))
		{
			String packageName = configuration.getPackage();
			if (packageName==null)
				packageName=sourcePackageName;

			if (!packageName.isEmpty())
				className=packageName+"."+className;
		}

		return className;
	}

	private Member createPropertyMemberIfValidProperty(BasicClazz clazz, DeclaredType interfaceOrClassMirrorType,
			                                           TypeMirror returnTypeMirror, List<? extends VariableElement> setterParams, List<? extends TypeMirror> setterParamTypes,
			                                           ExecutableElement methodElement, PropertyKind kind)
	{
		TypeMirror propertyTypeMirror;

		EnumSet<Modifier> declaredModifiers = EnumSet.noneOf(Modifier.class);

		if (kind==PropertyKind.GETTER) {
			if (setterParams.size()!=0) {
				if (!configuration.isMalformedPropertiesIgnored())
				  errorConsumer.message(methodElement, Kind.ERROR, String.format(ProcessorMessages.MalFormedGetter, methodElement.toString()));
				return null;
			}

			propertyTypeMirror = returnTypeMirror;

			String name = syntesisePropertyMemberName(configuration.getGetterPrefixes(), methodElement);

			return new Member(clazz, typeBuilder.createType(clazz, propertyTypeMirror, DetailLevel.High), name, declaredModifiers, createMemberAnnotations(clazz, name));
		} else if (kind==PropertyKind.IMMUTABLE_SETTER || kind==PropertyKind.MUTABLE_SETTER) {
			if (setterParams.size()!=1) {
				if (!configuration.isMalformedPropertiesIgnored())
  				  errorConsumer.message(methodElement, Kind.ERROR, String.format(ProcessorMessages.MalFormedSetter, methodElement.toString()));
				return null;
			}

			if (configuration.getMutability()==Mutability.Immutable && kind==PropertyKind.MUTABLE_SETTER)
			{
				errorConsumer.message(methodElement, Kind.ERROR, String.format(ProcessorMessages.MutableSetterNotAllowedForImmutableObject, methodElement.toString()));
				return null;
			} else if (configuration.getMutability()==Mutability.Mutable && kind==PropertyKind.IMMUTABLE_SETTER)
			{
				errorConsumer.message(methodElement, Kind.MANDATORY_WARNING, String.format(ProcessorMessages.ImmutableSetterNotExpectedForMutableObject, methodElement.toString()));
			}

			String returnTypeName = returnTypeMirror.toString();

			String declaredInterfaceTypeName = interfaceOrClassMirrorType.toString();
			if (!returnTypeName.equals("void") && !returnTypeName.equals(declaredInterfaceTypeName) && !returnTypeName.equals(clazz.getQualifiedName())) {
				if (!configuration.isMalformedPropertiesIgnored())
					  errorConsumer.message(methodElement, Kind.ERROR, String.format(ProcessorMessages.MalFormedSetter, methodElement.toString()));
				return null;
			}

			propertyTypeMirror=setterParamTypes.get(0);

			String name = syntesisePropertyMemberName(configuration.getSetterPrefixes(), methodElement);

			return new Member(clazz, typeBuilder.createType(clazz, propertyTypeMirror, DetailLevel.High), name, declaredModifiers, createMemberAnnotations(clazz, name));
		} else {
			return null; // Not a property.
		}
	}

	private List<Annotation> createMemberAnnotations(BasicClazz clazz, String memberName)
	{
		return configuration.getMemberAnnotations(m -> m.equals(memberName)).stream().map(pair -> new Annotation(clazz, pair.getValue())).collect(Collectors.toList());
	}

	private String syntesisePropertyMemberName(String[] propertyPrefixes, ExecutableElement method)
	{
		String name = method.getSimpleName().toString();

		int i=0;
		while (i<propertyPrefixes.length)
		{
			String prefix=propertyPrefixes[i++];
			int skip=prefix.length();
			if (name.startsWith(prefix) && name.length()>skip)
			{
				name=name.substring(skip);
				name=Introspector.decapitalize(name);
				name=NamesUtil.makeSafeJavaIdentifier(name);
				return name;
			}
		}

		return name;
	}

	private static boolean mustImplementComparable(Clazz clazz,  List<Method> methods)
	{
		return clazz.isOfType(comparableClass) && !instanceImplementationExists(clazz, "compareTo", methods);
	}

	private static boolean instanceImplementationExists(Clazz clazz, String methodName, List<Method> methods)
	{
      return methods.stream().anyMatch(m -> m.getDeclaringType()!=clazz && m.getName().equals(methodName) && !m.getDeclaredModifiers().contains(Modifier.STATIC) && !m.getDeclaredModifiers().contains(Modifier.ABSTRACT));
	}
}
