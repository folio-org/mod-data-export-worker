package org.folio.dew.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import one.util.streamex.StreamEx;

public class QueryUtils {

  public static final String ID = "id";
  private static final String CQL_COMBINE_OPERATOR = ") %s (";
  private static final String CQL_MATCH_STRICT = "%s==%s";
  private static final String CQL_MATCH = "%s=%s";
  private static final String CQL_PREFIX = "(";
  private static final String CQL_SUFFIX = ")";
  private static final String CQL_NEGATE_PREFIX = "cql.allRecords=1 NOT ";
  private static final String CQL_UNDEFINED_FIELD_EXPRESSION = CQL_NEGATE_PREFIX + "%s=\"\"";
  private static final Pattern CQL_SORT_BY_PATTERN = Pattern.compile("(.*)(\\ssortBy\\s.*)", Pattern.CASE_INSENSITIVE); //NOSONAR

  private QueryUtils() {}

  public static String encodeQuery(String query) {
    return URLEncoder.encode(query, StandardCharsets.UTF_8);
  }

  /**
   * Combines multiple CQL expressions using the specified logical operator. For example:<br>
   * Call: <code>combineCqlExpressions("and", "field1==value1", "field2==value2")</code><br>
   * Result: <code>(field1==value1) and (field2==value2)</code>
   *
   * @param operator    The logical operator to combine the expressions (e.g., "and", "or").
   * @param expressions The CQL expressions to combine.
   * @return A single CQL query string combining the provided expressions with the specified operator.
   */
  public static String combineCqlExpressions(String operator, String... expressions) {
    if (ArrayUtils.isEmpty(expressions)) {
      return StringUtils.EMPTY;
    }
    var sorting = StringUtils.EMPTY;
    // Check whether last expression contains sorting query. If it does, extract it to be added in the end of the resulting query
    Matcher matcher = CQL_SORT_BY_PATTERN.matcher(expressions[expressions.length - 1]);
    if (matcher.find()) {
      expressions[expressions.length - 1] = matcher.group(1);
      sorting = matcher.group(2);
    }

    var suffix = CQL_SUFFIX + sorting;
    var delimiter = String.format(CQL_COMBINE_OPERATOR, operator);
    return StreamEx.of(expressions)
      .filter(StringUtils::isNotBlank)
      .joining(delimiter, CQL_PREFIX, suffix);
  }

  /**
   * Converts a collection of IDs to a CQL query string using the specified ID field.
   *
   * @param ids     The collection of IDs to be converted.
   * @param idField The field name to be used in the CQL query.
   * @return A CQL query string representing the IDs.
   */
  public static String convertIdsToCqlQuery(Collection<String> ids, String idField) {
    return convertFieldListToCqlQuery(ids, idField, true, false);
  }

  /**
   * Converts a collection of IDs to a CQL query string using the default ID field.
   *
   * @param ids The collection of IDs to be converted.
   * @return A CQL query string representing the IDs.
   */
  public static String convertIdsToCqlQuery(Collection<String> ids) {
    return convertFieldListToCqlQuery(ids, ID, true, false);
  }

  /**
   * Transform list of values for some property to CQL query using 'or' operation and enclosing values with quotes
   *
   * @param values      list of field values
   * @param fieldName   the property name to search by
   * @param strictMatch indicates whether strict match mode (i.e. ==) should be used or not (i.e. =)
   * @return String representing CQL query to get records by some property enclosed values
   */
  public static String convertFieldListToEnclosedCqlQuery(Collection<?> values, String fieldName, boolean strictMatch) {
    return convertFieldListToCqlQuery(values, fieldName, strictMatch, true);
  }

  /**
   * Transform list of values for some property to CQL query using 'or' operation
   *
   * @param values      list of field values
   * @param fieldName   the property name to search by
   * @param strictMatch indicates whether strict match mode (i.e. ==) should be used or not (i.e. =)
   * @param enclosed    indicates whether values should be enclosed with quotes (i.e. asd) or not (i.e. "asd")
   * @return String representing CQL query to get records by some property values
   */
  public static String convertFieldListToCqlQuery(Collection<?> values, String fieldName, boolean strictMatch, boolean enclosed) {
    var prefix = String.format(strictMatch ? CQL_MATCH_STRICT : CQL_MATCH, fieldName, CQL_PREFIX);
    var enclose = enclosed ? "\"%s\"" : "%s";
    return StreamEx.of(values)
      .map(Object::toString)
      .map(enclose::formatted)
      .joining(" or ", prefix, CQL_SUFFIX);
  }

  public static String negateQuery(String cql) {
    return CQL_NEGATE_PREFIX + cql;
  }

  public static String getCqlExpressionForFieldNullValue(String fieldName) {
    return String.format(CQL_UNDEFINED_FIELD_EXPRESSION, fieldName);
  }

}
