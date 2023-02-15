package org.folio.dew.batch.bursarfeesfines.service;

import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.item.file.transform.FormatterLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BursarWriterBuilder<T> {

	private WritableResource resource;

	private String lineSeparator = System.getProperty("line.separator");

	private LineAggregator<T> lineAggregator;

  private String header;

	private String name;

	private DelimitedBuilder<T> delimitedBuilder;

	private FormattedBuilder<T> formattedBuilder;

  private LocalFilesStorage localFilesStorage;


  public BursarWriterBuilder<T> localFilesStorage(LocalFilesStorage localFilesStorage) {
    this.localFilesStorage = localFilesStorage;

    return this;
  }

	public BursarWriterBuilder<T> name(String name) {
		this.name = name;

		return this;
	}

	public BursarWriterBuilder<T> resource(WritableResource resource) {
		this.resource = resource;

		return this;
	}

	public BursarWriterBuilder<T> header(String header) {
		this.header = header;

		return this;
	}

	public FormattedBuilder<T> formatted() {
		this.formattedBuilder = new FormattedBuilder<>(this);
		return this.formattedBuilder;
	}

	public static class FormattedBuilder<T> {

		private final BursarWriterBuilder<T> parent;

		private String format;

		private final Locale locale = Locale.getDefault();

    private FieldExtractor<T> fieldExtractor;

		private final List<String> names = new ArrayList<>();

		protected FormattedBuilder(BursarWriterBuilder<T> parent) {
			this.parent = parent;
		}

		public FormattedBuilder<T> format(String format) {
			this.format = format;
			return this;
		}

		public BursarWriterBuilder<T> names(String... names) {
			this.names.addAll(Arrays.asList(names));
			return this.parent;
		}

		public FormatterLineAggregator<T> build() {

			FormatterLineAggregator<T> formatterLineAggregator = new FormatterLineAggregator<>();
			formatterLineAggregator.setFormat(this.format);
			formatterLineAggregator.setLocale(this.locale);
      int minimumLength = 0;
      formatterLineAggregator.setMinimumLength(minimumLength);
      int maximumLength = 0;
      formatterLineAggregator.setMaximumLength(maximumLength);

			if (this.fieldExtractor == null) {
				BeanWrapperFieldExtractor<T> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
				beanWrapperFieldExtractor.setNames(this.names.toArray(new String[0]));
				try {
					beanWrapperFieldExtractor.afterPropertiesSet();
				}
				catch (Exception e) {
					throw new IllegalStateException("Unable to initialize FormatterLineAggregator", e);
				}
				this.fieldExtractor = beanWrapperFieldExtractor;
			}

			formatterLineAggregator.setFieldExtractor(this.fieldExtractor);
			return formatterLineAggregator;
		}
	}

	public static class DelimitedBuilder<T> {

		private BursarWriterBuilder<T> parent;

		private List<String> names = new ArrayList<>();

		private String delimiter = ",";

		private FieldExtractor<T> fieldExtractor;

		protected DelimitedBuilder(BursarWriterBuilder<T> parent) {
			this.parent = parent;
		}


		public BursarWriterBuilder<T> names(String... names) {
			this.names.addAll(Arrays.asList(names));
			return this.parent;
		}

		public DelimitedLineAggregator<T> build() {

			DelimitedLineAggregator<T> delimitedLineAggregator = new DelimitedLineAggregator<>();
			if (this.delimiter != null) {
				delimitedLineAggregator.setDelimiter(this.delimiter);
			}

			if (this.fieldExtractor == null) {
				BeanWrapperFieldExtractor<T> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
				beanWrapperFieldExtractor.setNames(this.names.toArray(new String[0]));
				try {
					beanWrapperFieldExtractor.afterPropertiesSet();
				}
				catch (Exception e) {
					throw new IllegalStateException("Unable to initialize DelimitedLineAggregator", e);
				}
				this.fieldExtractor = beanWrapperFieldExtractor;
			}

			delimitedLineAggregator.setFieldExtractor(this.fieldExtractor);
			return delimitedLineAggregator;
		}
	}

	public BursarWriter<T> build() {

		BursarWriter<T> writer = new BursarWriter<>();

		writer.setName(this.name);
		writer.setHeader(this.header);
		if (this.lineAggregator == null) {
					if (this.delimitedBuilder != null) {
				this.lineAggregator = this.delimitedBuilder.build();
			}
			else {
				this.lineAggregator = this.formattedBuilder.build();
			}
		}
		writer.setLineAggregator(this.lineAggregator);
		writer.setLineSeparator(this.lineSeparator);
		writer.setResource(this.resource);
    writer.setLocalFilesStorage(this.localFilesStorage);

		return writer;
	}
}
