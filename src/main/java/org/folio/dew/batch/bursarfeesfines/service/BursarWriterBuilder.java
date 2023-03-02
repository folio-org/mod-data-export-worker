package org.folio.dew.batch.bursarfeesfines.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.folio.dew.repository.LocalFilesStorage;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.FieldExtractor;
import org.springframework.batch.item.file.transform.FormatterLineAggregator;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.WritableResource;

public class BursarWriterBuilder {

  private WritableResource resource;

  private LineAggregator<String> lineAggregator;

  private String header;
  private String footer;

  private String name;

  private DelimitedBuilder delimitedBuilder;

  private FormattedBuilder formattedBuilder;

  private LocalFilesStorage localFilesStorage;

  public BursarWriterBuilder localFilesStorage(
    LocalFilesStorage localFilesStorage
  ) {
    this.localFilesStorage = localFilesStorage;

    return this;
  }

  public BursarWriterBuilder name(String name) {
    this.name = name;

    return this;
  }

  public BursarWriterBuilder resource(WritableResource resource) {
    this.resource = resource;

    return this;
  }

  public BursarWriterBuilder header(String header) {
    this.header = header;

    return this;
  }

  public BursarWriterBuilder footer(String footer) {
    this.footer = footer;

    return this;
  }

  public FormattedBuilder formatted() {
    this.formattedBuilder = new FormattedBuilder(this);
    return this.formattedBuilder;
  }

  public static class FormattedBuilder {

    private final BursarWriterBuilder parent;

    private String format;

    private final Locale locale = Locale.getDefault();

    private FieldExtractor<String> fieldExtractor;

    private final List<String> names = new ArrayList<>();

    protected FormattedBuilder(BursarWriterBuilder parent) {
      this.parent = parent;
    }

    public FormattedBuilder format(String format) {
      this.format = format;
      return this;
    }

    public BursarWriterBuilder names(String... names) {
      this.names.addAll(Arrays.asList(names));
      return this.parent;
    }

    public FormatterLineAggregator<String> build() {
      FormatterLineAggregator<String> formatterLineAggregator = new FormatterLineAggregator<>();
      formatterLineAggregator.setFormat(this.format);
      formatterLineAggregator.setLocale(this.locale);
      int minimumLength = 0;
      formatterLineAggregator.setMinimumLength(minimumLength);
      int maximumLength = 0;
      formatterLineAggregator.setMaximumLength(maximumLength);

      if (this.fieldExtractor == null) {
        BeanWrapperFieldExtractor<String> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
        beanWrapperFieldExtractor.setNames(this.names.toArray(new String[0]));
        try {
          beanWrapperFieldExtractor.afterPropertiesSet();
        } catch (Exception e) {
          throw new IllegalStateException(
            "Unable to initialize FormatterLineAggregator",
            e
          );
        }
        this.fieldExtractor = beanWrapperFieldExtractor;
      }

      formatterLineAggregator.setFieldExtractor(this.fieldExtractor);
      return formatterLineAggregator;
    }
  }

  public static class DelimitedBuilder {

    private BursarWriterBuilder parent;

    private List<String> names = new ArrayList<>();

    private String delimiter = ",";

    private FieldExtractor<String> fieldExtractor;

    protected DelimitedBuilder(BursarWriterBuilder parent) {
      this.parent = parent;
    }

    public BursarWriterBuilder names(String... names) {
      this.names.addAll(Arrays.asList(names));
      return this.parent;
    }

    public DelimitedLineAggregator<String> build() {
      DelimitedLineAggregator<String> delimitedLineAggregator = new DelimitedLineAggregator<>();
      if (this.delimiter != null) {
        delimitedLineAggregator.setDelimiter(this.delimiter);
      }

      if (this.fieldExtractor == null) {
        BeanWrapperFieldExtractor<String> beanWrapperFieldExtractor = new BeanWrapperFieldExtractor<>();
        beanWrapperFieldExtractor.setNames(this.names.toArray(new String[0]));
        try {
          beanWrapperFieldExtractor.afterPropertiesSet();
        } catch (Exception e) {
          throw new IllegalStateException(
            "Unable to initialize DelimitedLineAggregator",
            e
          );
        }
        this.fieldExtractor = beanWrapperFieldExtractor;
      }

      delimitedLineAggregator.setFieldExtractor(this.fieldExtractor);
      return delimitedLineAggregator;
    }
  }

  public BursarWriter build() {
    BursarWriter writer = new BursarWriter();

    writer.setName(this.name);
    writer.setHeader(this.header);
    writer.setFooter(this.footer);
    if (this.lineAggregator == null) {
      if (this.delimitedBuilder != null) {
        this.lineAggregator = this.delimitedBuilder.build();
      } else {
        this.lineAggregator = this.formattedBuilder.build();
      }
    }
    writer.setLineAggregator(this.lineAggregator);
    writer.setResource(this.resource);
    writer.setLocalFilesStorage(this.localFilesStorage);

    return writer;
  }
}
