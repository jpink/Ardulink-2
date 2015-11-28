package com.github.pfichtner.beans;

import static com.github.pfichtner.beans.finder.impl.FindByIntrospection.beanAttributes;

import com.github.pfichtner.beans.Attribute.AttributeReader;
import com.github.pfichtner.beans.Attribute.AttributeWriter;
import com.github.pfichtner.beans.finder.api.AttributeFinder;

public class BeanProperties {

	public static class Builder {

		private final Object bean;
		private AttributeFinder[] finders;

		public Builder(Object bean) {
			this.bean = bean;
		}

		public BeanProperties build() {
			return new BeanProperties(this);
		}

		public Builder using(AttributeFinder... finders) {
			this.finders = finders.clone();
			return this;
		}

	}

	public static class DefaultAttribute implements Attribute {

		private final String name;
		private final Class<?> type;
		private final AttributeReader reader;
		private final AttributeWriter writer;

		public DefaultAttribute(String name, Class<?> type,
				AttributeReader reader, AttributeWriter writer) {
			this.name = name;
			this.type = type;
			this.reader = reader;
			this.writer = writer;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Class<?> getType() {
			return this.type;
		}

		@Override
		public Object readValue() throws Exception {
			return reader.getValue();
		}

		@Override
		public void writeValue(Object value) throws Exception {
			writer.setValue(value);
		}

	}

	private final Object bean;
	private final AttributeFinder[] finders;

	private BeanProperties(final Builder builder) {
		this.bean = builder.bean;
		this.finders = builder.finders.clone();
	}

	public static BeanProperties forBean(final Object bean) {
		return builder(bean).using(beanAttributes()).build();
	}

	public static BeanProperties.Builder builder(final Object bean) {
		return new BeanProperties.Builder(bean);
	}

	public Attribute getAttribute(final String name) {
		try {
			AttributeReader reader = findReader(name);
			AttributeWriter writer = findWriter(name);
			Class<?> type = determineType(reader, writer);
			return (reader == null && writer == null) || type == null ? null
					: new DefaultAttribute(name, type, reader, writer);
		} catch (final Exception e) {
			return null;
		}
	}

	private static Class<?> determineType(AttributeReader reader,
			AttributeWriter writer) {
		Class<?> readerType = reader == null ? null : reader.getType();
		Class<?> writerType = writer == null ? null : writer.getType();
		if (readerType == null) {
			return writerType;
		}
		if (writerType == null) {
			return readerType;
		}
		if (readerType.isAssignableFrom(writerType)) {
			return readerType;
		}
		if (writerType.isAssignableFrom(readerType)) {
			return writerType;
		}
		return null;
	}

	private AttributeReader findReader(final String name) throws Exception {
		for (AttributeFinder finder : finders) {
			AttributeReader reader = finder.findReader(bean, name);
			if (reader != null) {
				return reader;
			}
		}
		return null;
	}

	private AttributeWriter findWriter(final String name) throws Exception {
		for (AttributeFinder finder : finders) {
			AttributeWriter writer = finder.findWriter(bean, name);
			if (writer != null) {
				return writer;
			}
		}
		return null;
	}

}
