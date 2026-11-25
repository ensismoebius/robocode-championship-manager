package utils.robocode;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * This program is licensed under GPLv3 see the README file for more information
 *
 * Reads a compiled class and tells what it uses of the Java library that the Java running this program does not have. A robot
 * compiled with a newer Java has its classes marked for the Java of the battles (see {@link RobotJar#forJava}), and runs, unless it
 * calls something of the library that only the newer Java has: then it would fail in the middle of a battle. The class is read from
 * its constant pool, where every class, method and field it uses is named, and each one of the library (java.* and javax.*) is
 * looked up in the Java that runs here, by its name and its types.
 *
 * @author André Furlan - ensismoebius@gmail.com
 */
final class ClassCheck {

	/** The kinds of the entries of the constant pool that are read. */
	private static final int UTF8 = 1;
	private static final int CLASS = 7;
	private static final int FIELD = 9;
	private static final int METHOD = 10;
	private static final int INTERFACE_METHOD = 11;
	/** The minor version of a class that uses the preview features of its Java. */
	static final int PREVIEW = 0xffff;

	/** What a class is: its versions, and the first thing of the library it uses that this Java does not have, or null. */
	record Report(int major, int minor, String missing) {
	}

	/** Only static methods: this class is not instantiated. */
	private ClassCheck() {
	}

	/**
	 * Reads a class file.
	 *
	 * @return what it is, or null when the bytes are not a class
	 * @throws IOException
	 *             when the class is cut or its constant pool cannot be read
	 */
	static Report read(byte[] content) throws IOException {
		if (content.length < 10 || (content[0] & 0xff) != 0xca || (content[1] & 0xff) != 0xfe || (content[2] & 0xff) != 0xba || (content[3] & 0xff) != 0xbe) {
			return null;
		}
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(content, 4, content.length - 4));
		int minor = in.readUnsignedShort();
		int major = in.readUnsignedShort();
		int count = in.readUnsignedShort();
		int[] tags = new int[count];
		int[] first = new int[count];
		int[] second = new int[count];
		String[] texts = new String[count];
		for (int index = 1; index < count; index++) {
			int tag = in.readUnsignedByte();
			tags[index] = tag;
			switch (tag) {
			case UTF8 -> texts[index] = in.readUTF();
			case 3, 4 -> in.readInt();
			// A long or a double takes two places of the pool
			case 5, 6 -> {
				in.readLong();
				index++;
			}
			case CLASS, 8, 16, 19, 20 -> first[index] = in.readUnsignedShort();
			case FIELD, METHOD, INTERFACE_METHOD, 12, 17, 18 -> {
				first[index] = in.readUnsignedShort();
				second[index] = in.readUnsignedShort();
			}
			case 15 -> {
				in.readUnsignedByte();
				in.readUnsignedShort();
			}
			default -> throw new IOException("constant pool entry of unknown kind " + tag);
			}
		}
		return new Report(major, minor, missing(tags, first, second, texts));
	}

	/** The first class, method or field of the library named in the pool that this Java does not have, or null. */
	private static String missing(int[] tags, int[] first, int[] second, String[] texts) {
		for (int index = 1; index < tags.length; index++) {
			String found = null;
			if (tags[index] == CLASS) {
				found = missingClass(texts[first[index]]);
			} else if (tags[index] == FIELD || tags[index] == METHOD || tags[index] == INTERFACE_METHOD) {
				String owner = texts[first[first[index]]];
				int nameAndType = second[index];
				found = missingMember(owner, texts[first[nameAndType]], texts[second[nameAndType]], tags[index] == FIELD);
			}
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/** Whether the internal name (java/util/List) is of the library: java.* or javax.*. */
	private static boolean ofTheLibrary(String name) {
		return name.startsWith("java/") || name.startsWith("javax/");
	}

	/** The class of the library with the internal name, as this Java has it, or null when it does not have it. */
	private static Class<?> library(String name) {
		try {
			return Class.forName(name.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
		} catch (ClassNotFoundException | LinkageError absent) {
			return null;
		}
	}

	/** The class named, when it is of the library and this Java does not have it; an array is its element. */
	private static String missingClass(String name) {
		String element = name;
		while (element.startsWith("[")) {
			element = element.substring(1);
		}
		if (element.startsWith("L") && element.endsWith(";")) {
			element = element.substring(1, element.length() - 1);
		}
		return ofTheLibrary(element) && library(element) == null ? element.replace('/', '.') : null;
	}

	/** The member named, when its class is of the library and this Java does not have the member with those types. */
	private static String missingMember(String owner, String name, String descriptor, boolean field) {
		if (!ofTheLibrary(owner)) {
			return null;
		}
		Class<?> type = library(owner);
		if (type == null) {
			return owner.replace('/', '.');
		}
		String shown = owner.replace('/', '.') + "." + name + (field ? "" : "()");
		if (field) {
			return hasField(type, name) ? null : shown;
		}
		// The handles take any types: only the name counts
		if (type == java.lang.invoke.MethodHandle.class || type == java.lang.invoke.VarHandle.class) {
			return Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.getName().equals(name)) ? null : shown;
		}
		MethodType wanted;
		try {
			wanted = MethodType.fromMethodDescriptorString(descriptor, ClassLoader.getPlatformClassLoader());
		} catch (TypeNotPresentException | IllegalArgumentException notOfTheLibrary) {
			// A type that is not of the library (the robot's own) or that this Java does not have: the classes are checked one by one
			return null;
		}
		return hasMethod(type, name, wanted) ? null : shown;
	}

	/** Whether the class, or one it extends or implements, declares the field. */
	private static boolean hasField(Class<?> type, String name) {
		for (Class<?> each : lineage(type)) {
			for (Field field : each.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	/** Whether the class, or one it extends or implements, declares the method, or the constructor, with those types. */
	private static boolean hasMethod(Class<?> type, String name, MethodType wanted) {
		if (name.equals("<init>")) {
			for (Constructor<?> constructor : type.getDeclaredConstructors()) {
				if (Arrays.equals(constructor.getParameterTypes(), wanted.parameterArray())) {
					return true;
				}
			}
			return false;
		}
		for (Class<?> each : lineage(type)) {
			for (Method method : each.getDeclaredMethods()) {
				if (method.getName().equals(name) && method.getReturnType() == wanted.returnType() && Arrays.equals(method.getParameterTypes(), wanted.parameterArray())) {
					return true;
				}
			}
		}
		return false;
	}

	/** The class, all the classes it extends and all the interfaces it implements, and Object. */
	private static Set<Class<?>> lineage(Class<?> type) {
		Set<Class<?>> seen = new HashSet<>();
		Deque<Class<?>> waiting = new ArrayDeque<>();
		waiting.add(type);
		waiting.add(Object.class);
		while (!waiting.isEmpty()) {
			Class<?> each = waiting.poll();
			if (seen.add(each)) {
				if (each.getSuperclass() != null) {
					waiting.add(each.getSuperclass());
				}
				waiting.addAll(Arrays.asList(each.getInterfaces()));
			}
		}
		return seen;
	}
}
