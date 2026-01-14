/**
 * Copyright (c) 2025 TigerGraph Inc.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tigergraph.spark.write.upsert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tigergraph.spark.util.Utils;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DateType;
import org.apache.spark.sql.types.IntegerType;
import org.apache.spark.sql.types.LongType;
import org.apache.spark.sql.types.ShortType;
import org.apache.spark.sql.types.StringType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.TimestampNTZType;
import org.apache.spark.sql.types.TimestampType;
import org.slf4j.Logger;

/**
 * Abstract base class for building JSON for TigerGraph upsert operations.
 *
 * <p>This class provides a framework for constructing JSON objects that conform to TigerGraph's
 * upsert endpoints format, supporting both vertex and edge operations. It handles the conversion of
 * Spark DataFrame rows to the appropriate JSON structure, including attribute mappings and
 * operations.
 *
 * <p>TigerGraph's upsert format follows a nested structure:
 *
 * <p><strong>Vertex JSON Structure:</strong>
 *
 * <pre>{@code
 * {
 *   "vertices": {
 *     "vertex_type": {
 *       "vertex_id1": {
 *         "attribute1": {"value": value1},
 *         "attribute2": {"value": value2, "op": "op2"}
 *       },
 *       "vertex_id2": { ... }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Edge JSON Structure:</strong>
 *
 * <pre>{@code
 * {
 *   "edges": {
 *     "source_vertex_type": {
 *       "source_id1": {
 *         "edge_type": {
 *           "target_vertex_type": {
 *             "target_id1": {
 *               "attribute1": {"value": value1},
 *               "attribute2": {"value": value2, "op": "op2"}
 *             }
 *           }
 *         }
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p><strong>Discriminated Edge JSON Structure:</strong>
 *
 * <pre>{@code
 * {
 *   "edges": {
 *     "source_vertex_type": {
 *       "source_id1": {
 *         "edge_type": {
 *           "target_vertex_type": {
 *             "target_id1": [
 *               {
 *                 "discriminator1": {"value": "value1"},
 *                 "attribute1": {"value": 10}
 *               },
 *               {
 *                 "discriminator1": {"value": "value2"},
 *                 "attribute1": {"value": 20}
 *               }
 *             ]
 *           }
 *         }
 *       }
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>The class provides concrete implementations for both vertex and edge operations through the
 * nested {@link VertexJsonBuilder} and {@link EdgeJsonBuilder} classes.
 */
public abstract class UpsertJsonBuilder {
  protected static final ObjectMapper mapper = new ObjectMapper();
  protected static final Logger logger =
      com.tigergraph.spark.log.LoggerFactory.getLogger(UpsertJsonBuilder.class);

  protected List<AttributeMapper> attributeMappers;
  protected final StructType schema;
  protected final ObjectNode rootNode;
  protected final String attributeMapping;
  protected final String attributeOp;

  // ---------- CONSTRUCTORS AND INITIALIZATION ----------//

  /**
   * Constructor for UpsertJsonBuilder.
   *
   * @param attributeMapping JSON string defining the attribute mappings in the format
   *     "column1:attribute1,column2:attribute2". If empty, column names will be used as attribute
   *     names.
   * @param attributeOp JSON string defining the attribute operations in the format
   *     "attribute1:op1,attribute2:op2", where operations can be "sum", "min", "max", etc. If
   *     empty, no operations will be applied.
   * @param schema The schema of the data.
   */
  protected UpsertJsonBuilder(String attributeMapping, String attributeOp, StructType schema) {
    this.schema = schema;
    this.attributeMapping = attributeMapping;
    this.attributeOp = attributeOp;
    this.rootNode = mapper.createObjectNode();
    this.attributeMappers = new ArrayList<>();
  }

  // ---------- PUBLIC API ----------//

  /**
   * Adds a row to the JSON structure for upsert operations.
   *
   * <p>This method first converts the row's attributes to JSON format, then delegates to the
   * subclass implementation of {@link #addObject(InternalRow, JsonNode)} to add the specific object
   * structure (vertex or edge) to the JSON.
   *
   * @param row The data row containing values for the attributes.
   */
  public final void add(InternalRow row) {
    JsonNode attributesNode = buildAttributesJson(this.attributeMappers, row);
    addObject(row, attributesNode);
  }

  /**
   * Builds the final JSON structure for upsert operations.
   *
   * <p>This method returns the complete JSON object that can be sent to TigerGraph's upsert
   * endpoint.
   *
   * @return The root JSON node containing all added objects.
   */
  public ObjectNode build() {
    return rootNode;
  }

  /**
   * Clears the JSON structure, removing all previously added objects.
   *
   * <p>This method allows reusing the same builder instance for multiple batches of data.
   */
  public abstract void clear();

  // ---------- CORE UTILITY METHODS ----------//

  /**
   * Builds the attributes JSON node from the provided attribute mappings and row data.
   *
   * <p>This method converts attribute values from the Spark row into the format expected by
   * TigerGraph, including any operations that should be applied to the attributes.
   *
   * <p>Example output:
   *
   * <pre>{@code
   * {
   *   "name": {"value": "John"},
   *   "age": {"value": 30},
   *   "score": {"value": 95, "op": "sum"}
   * }
   * }</pre>
   *
   * @param attributeMappers List of attribute mappers to use for building the JSON.
   * @param row The data row containing values for the attributes.
   * @return A JsonNode representing the attributes.
   */
  protected static JsonNode buildAttributesJson(
      List<AttributeMapper> attributeMappers, InternalRow row) {
    ObjectNode attributesRootNode = mapper.createObjectNode();
    for (AttributeMapper attr : attributeMappers) {
      ObjectNode attrNode = attr.getAttributeValue(row);
      if (attrNode != null) {
        attributesRootNode.set(attr.getAttributeName(), attrNode);
      }
    }
    return attributesRootNode;
  }

  /**
   * Creates attribute mappers based on the provided attribute mapping configuration.
   *
   * <p>This method is implemented by subclasses to create the appropriate attribute mappers for
   * their specific context (vertex or edge), potentially excluding certain fields that are used for
   * other purposes (e.g., ID fields).
   *
   * @return List of AttributeMapper objects.
   */
  protected abstract List<AttributeMapper> createAttributeMappers();

  /**
   * Adds an object to the JSON structure based on the provided row data.
   *
   * <p>This method is implemented by subclasses to add the specific object structure (vertex or
   * edge) to the JSON.
   *
   * @param row The data row containing values for the attributes.
   * @param attributesNode The JSON node representing the attributes to be added.
   */
  protected abstract void addObject(InternalRow row, JsonNode attributesNode);

  /**
   * Creates attribute mappers based on the provided attributeMapping and attributeOp options and
   * schema.
   *
   * <p>This method parses the attribute mapping and operation strings to create a list of {@link
   * AttributeMapper} objects that can convert Spark row values to TigerGraph attribute format.
   *
   * <p>If attributeMapping is not empty, it is used to map column names to attribute names.
   * Otherwise, column names are used as attribute names.
   *
   * <p>If attributeOp is not empty, it is used to define operations for attributes.
   *
   * @param attributeMapping String defining the attribute mappings in the format
   *     "column1:attribute1,column2:attribute2".
   * @param attributeOp String defining the attribute operations in the format
   *     "attribute1:op1,attribute2:op2".
   * @param schema The schema of the data.
   * @param excludeFields Set of field names to exclude from the mappings (e.g., ID fields).
   * @return List of AttributeMapper objects.
   * @throws IllegalArgumentException if the attribute mapping or fields are invalid.
   */
  protected static List<AttributeMapper> createAttributeMappers(
      String attributeMapping, String attributeOp, StructType schema, Set<String> excludeFields) {
    List<AttributeMapper> attributeMappers = new ArrayList<>();
    Map<String, String> attributeMappingMap = new HashMap<>();
    try {
      attributeMappingMap = Utils.parseKeyValueString(attributeMapping);
    } catch (Exception e) {
      logger.error("Invalid attribute mapping: " + attributeMapping, e);
      throw new IllegalArgumentException(
          "Invalid attribute mapping: "
              + attributeMapping
              + ", expected format: 'column1:attribute1,column2:attribute2'",
          e);
    }
    Map<String, String> attributeOpMap = Utils.parseKeyValueString(attributeOp);

    // If attribute mapping is provided, use it to map column names to attribute names
    if (!attributeMappingMap.isEmpty()) {
      for (Map.Entry<String, String> entry : attributeMappingMap.entrySet()) {
        String fieldName = entry.getKey();
        if (excludeFields != null && excludeFields.contains(fieldName)) {
          continue;
        }
        int fieldIndex;
        try {
          fieldIndex = schema.fieldIndex(fieldName);
        } catch (Exception e) {
          logger.error(
              "Field '" + fieldName + "' specified in attribute mapping not found in schema", e);
          throw new IllegalArgumentException(
              "Field '" + fieldName + "' specified in attribute mapping not found in schema", e);
        }
        StructField field = schema.fields()[fieldIndex];
        attributeMappers.add(
            new AttributeMapper(
                fieldIndex, field, entry.getValue(), attributeOpMap.get(entry.getValue())));
      }
    } else {
      // Otherwise, use column names as attribute names
      for (int i = 0; i < schema.fields().length; i++) {
        StructField field = schema.fields()[i];
        String fieldName = field.name();
        if (excludeFields != null && excludeFields.contains(fieldName)) {
          continue;
        }
        attributeMappers.add(
            new AttributeMapper(i, field, fieldName, attributeOpMap.get(fieldName)));
      }
    }
    return attributeMappers;
  }

  // ---------- ID FIELD HANDLING ----------//

  /**
   * Creates a composite key extractor function for the given ID fields.
   *
   * <p>This method pre-validates field types and creates optimized extractors to avoid type
   * checking on every call. It supports both single ID fields and composite keys (multiple fields
   * joined by commas).
   *
   * @param idFields List of field names to extract from the row
   * @param schema The schema to validate field types
   * @return A function that can build composite keys from InternalRow data
   * @throws IllegalArgumentException if any field type is not supported for ID
   */
  protected static Function<InternalRow, String> createCompositeKeyExtractor(
      List<String> idFields, StructType schema) {
    if (idFields.isEmpty()) {
      return row -> null;
    }

    // Pre-validate types and create extractors
    List<Map.Entry<Integer, IdFieldExtractor>> extractors =
        idFields.stream()
            .map(
                fieldName -> {
                  int fieldIndex = schema.fieldIndex(fieldName);
                  DataType fieldType = schema.fields()[fieldIndex].dataType();

                  // Validate that the field type is supported for ID
                  if (!isValidIdFieldType(fieldType)) {
                    throw new IllegalArgumentException(
                        "Unsupported ID field type for field '"
                            + fieldName
                            + "': "
                            + fieldType.simpleString()
                            + ". Only STRING, INTEGER, LONG, SHORT, DATE, and TIMESTAMP types are"
                            + " supported for ID fields.");
                  }

                  // Create type-specific extractor
                  IdFieldExtractor extractor = createIdFieldExtractor(fieldType);
                  return new AbstractMap.SimpleEntry<>(fieldIndex, extractor);
                })
            .collect(Collectors.toList());

    // Create indices array for null checking
    int[] fieldIndices = idFields.stream().mapToInt(schema::fieldIndex).toArray();

    // Return optimized function
    return row -> {
      // Check if any field is null first
      for (int fieldIndex : fieldIndices) {
        if (row.isNullAt(fieldIndex)) {
          return null;
        }
      }

      // Extract values using pre-created extractors
      return extractors.stream()
          .map(
              entry -> {
                return entry.getValue().extract(row, entry.getKey());
              })
          .collect(Collectors.joining(","));
    };
  }

  /**
   * Validates if a data type is supported for ID fields.
   *
   * @param dataType The data type to validate
   * @return true if the type is supported for ID fields
   */
  private static boolean isValidIdFieldType(DataType dataType) {
    return dataType instanceof StringType
        || dataType instanceof IntegerType
        || dataType instanceof LongType
        || dataType instanceof ShortType
        || dataType instanceof DateType
        || dataType instanceof TimestampType
        || dataType instanceof TimestampNTZType;
  }

  /**
   * Validates that all ID fields have supported types.
   *
   * <p>This method checks that all fields in the provided list have types that are supported for
   * use as ID fields in TigerGraph.
   *
   * @param idFields List of field names to validate
   * @param schema The schema to validate against
   * @throws IllegalArgumentException if any field type is not supported for ID
   */
  private static void validateIdFieldTypes(List<String> idFields, StructType schema) {
    for (String fieldName : idFields) {
      int fieldIndex = schema.fieldIndex(fieldName);
      DataType fieldType = schema.fields()[fieldIndex].dataType();

      if (!isValidIdFieldType(fieldType)) {
        throw new IllegalArgumentException(
            "Unsupported ID field type for field '"
                + fieldName
                + "': "
                + fieldType.simpleString()
                + ". Only STRING, INTEGER, LONG, SHORT, DATE, and TIMESTAMP types are supported for"
                + " ID fields.");
      }
    }
  }

  /**
   * Creates an ID field extractor function based on the field data type.
   *
   * <p>This method returns a function that can extract a value from a specific field in an {@link
   * InternalRow} and convert it to a string representation suitable for use as an ID.
   *
   * @param fieldType The data type of the field
   * @return A function that extracts and converts the field value to string
   * @throws IllegalArgumentException if the field type is not supported for ID
   */
  private static IdFieldExtractor createIdFieldExtractor(DataType fieldType) {
    if (fieldType instanceof StringType) {
      return (row, fieldIndex) -> row.getUTF8String(fieldIndex).toString();
    } else if (fieldType instanceof IntegerType) {
      return (row, fieldIndex) -> String.valueOf(row.getInt(fieldIndex));
    } else if (fieldType instanceof LongType) {
      return (row, fieldIndex) -> String.valueOf(row.getLong(fieldIndex));
    } else if (fieldType instanceof ShortType) {
      return (row, fieldIndex) -> String.valueOf(row.getShort(fieldIndex));
    } else if (fieldType instanceof DateType) {
      // Spark stores dates as days since epoch as Int
      return (row, fieldIndex) ->
          String.valueOf(AttributeMapper.convertSparkDateToDatetime(row.getInt(fieldIndex)));
    } else if (fieldType instanceof TimestampType || fieldType instanceof TimestampNTZType) {
      // Spark stores timestamps as microseconds since epoch
      return (row, fieldIndex) ->
          String.valueOf(AttributeMapper.convertSparkTimestampToDatetime(row.getLong(fieldIndex)));
    } else {
      // This should never happen due to validation above
      throw new IllegalArgumentException("Unsupported ID field type: " + fieldType.simpleString());
    }
  }

  /**
   * Functional interface for extracting and converting ID field values to strings.
   *
   * <p>This interface is used to create type-specific extractors for different field types to avoid
   * type checking on every row.
   */
  @FunctionalInterface
  private interface IdFieldExtractor {
    /**
     * Extracts a value from a row and converts it to a string.
     *
     * @param row The row to extract from
     * @param fieldIndex The index of the field to extract
     * @return A string representation of the field value
     */
    String extract(InternalRow row, int fieldIndex);
  }

  // ---------- VERTEX JSON BUILDER ----------//

  /**
   * Builder for constructing vertex JSON for TigerGraph upsert operations.
   *
   * <p>This class creates JSON in the following format:
   *
   * <pre>{@code
   * {
   *   "vertices": {
   *     "person": {
   *       "p1": { "name": {"value": "John"}, "age": {"value": 30} },
   *       "p2": { "name": {"value": "Jane"}, "age": {"value": 25} }
   *     }
   *   }
   * }
   * }</pre>
   */
  public static final class VertexJsonBuilder extends UpsertJsonBuilder {
    // This field is used in the constructor to set up the JSON structure
    private final String vertexType;
    private final List<String> vertexIdFields;
    private final Function<InternalRow, String> vertexIdExtractor;
    private final ObjectNode vertices;
    private final ObjectNode vertexTypeNode;

    /**
     * Constructor for VertexJsonBuilder.
     *
     * @param vertexType The type of the vertex in the TigerGraph schema.
     * @param vertexIdFields List of field names to extract from the row for vertex ID. For
     *     composite keys, multiple field values will be joined with commas.
     * @param attributeMapping JSON string defining the attribute mappings in the format
     *     "column1:attribute1,column2:attribute2".
     * @param attributeOp String defining the attribute operations in the format
     *     "attribute1:op1,attribute2:op2".
     * @param schema The schema of the data.
     * @throws IllegalArgumentException if any ID field type is not supported
     */
    public VertexJsonBuilder(
        String vertexType,
        List<String> vertexIdFields,
        String attributeMapping,
        String attributeOp,
        StructType schema) {
      super(attributeMapping, attributeOp, schema);
      this.vertexType = vertexType;
      this.vertexIdFields = vertexIdFields;
      this.vertexIdExtractor = createCompositeKeyExtractor(vertexIdFields, schema);

      // Validate ID field types during construction
      validateIdFieldTypes(vertexIdFields, schema);

      this.vertices = mapper.createObjectNode();
      this.rootNode.set("vertices", this.vertices);
      this.vertexTypeNode = mapper.createObjectNode();
      this.vertices.set(vertexType, this.vertexTypeNode);
      this.attributeMappers = createAttributeMappers();
    }

    /** Clears the vertex type node, removing all previously added vertices. */
    @Override
    public void clear() {
      vertexTypeNode.removeAll();
    }

    /**
     * Creates attribute mappers for vertex attributes, excluding ID fields.
     *
     * @return List of AttributeMapper objects for vertex attributes
     */
    @Override
    protected List<AttributeMapper> createAttributeMappers() {
      Set<String> excludeFields = new HashSet<>(vertexIdFields);
      return createAttributeMappers(attributeMapping, attributeOp, schema, excludeFields);
    }

    /**
     * Adds a vertex object to the JSON structure based on the provided row data.
     *
     * <p>This method extracts the vertex ID from the row and adds the vertex with its attributes to
     * the JSON structure.
     *
     * @param row The data row containing values for the vertex ID and attributes
     * @param attributesNode The JSON node representing the vertex attributes
     */
    @Override
    protected void addObject(InternalRow row, JsonNode attributesNode) {
      String vertexId = vertexIdExtractor.apply(row);
      if (vertexId == null) {
        logger.warn("Vertex ID is null, skipping this row");
        return;
      }
      vertexTypeNode.set(vertexId, attributesNode);
    }
  }

  // ---------- EDGE JSON BUILDER ----------//

  /**
   * Builder for constructing edge JSON for TigerGraph upsert operations.
   *
   * <p>This class creates JSON in the following format:
   *
   * <pre>{@code
   * {
   *   "edges": {
   *     "person": {
   *       "p1": {
   *         "knows": {
   *           "person": {
   *             "p2": { "since": {"value": "2020-01-01"}, "weight": {"value": 0.8} }
   *           }
   *         }
   *       }
   *     }
   *   }
   * }
   * }</pre>
   *
   * <p>It also supports discriminated edges, where multiple edges of the same type can exist
   * between the same source and target vertices, differentiated by attribute values.
   */
  public static final class EdgeJsonBuilder extends UpsertJsonBuilder {
    private final String edgeType;
    private final String sourceType;
    private final List<String> sourceIdFields;
    private final Function<InternalRow, String> sourceIdExtractor;
    private final String targetType;
    private final List<String> targetIdFields;
    private final Function<InternalRow, String> targetIdExtractor;
    private final Set<String> discriminators;
    private final ObjectNode edges;

    /**
     * Constructor for EdgeJsonBuilder.
     *
     * <p>Example usage:
     *
     * <pre>{@code
     * EdgeJsonBuilder builder = new EdgeJsonBuilder(
     *     "knows",                       // Edge type
     *     "person", Arrays.asList("id1"), // Source vertex type and ID field(s)
     *     "person", Arrays.asList("id2"), // Target vertex type and ID field(s)
     *     "since:since,weight:weight",    // Attribute mapping
     *     "weight:sum",                   // Attribute operations
     *     schema,                         // Spark schema
     *     Set.of("since")                 // Discriminator attributes
     * );
     * }</pre>
     *
     * @param edgeType The type of the edge in the TigerGraph schema.
     * @param sourceType The type of the source vertex.
     * @param sourceIdFields List of field names to extract from the row for source vertex ID.
     * @param targetType The type of the target vertex.
     * @param targetIdFields List of field names to extract from the row for target vertex ID.
     * @param attributeMapping JSON string defining the attribute mappings.
     * @param attributeOp String defining the attribute operations.
     * @param schema The schema of the data.
     * @param discriminators Set of attribute names to use as discriminators for the edge. If
     *     provided, allows multiple edges of the same type between the same source and target
     *     vertices, differentiated by these attributes.
     * @throws IllegalArgumentException if any ID field type is not supported
     */
    public EdgeJsonBuilder(
        String edgeType,
        String sourceType,
        List<String> sourceIdFields,
        String targetType,
        List<String> targetIdFields,
        String attributeMapping,
        String attributeOp,
        StructType schema,
        Set<String> discriminators) {
      super(attributeMapping, attributeOp, schema);
      this.edgeType = edgeType;
      this.sourceType = sourceType;
      this.sourceIdFields = sourceIdFields;
      this.sourceIdExtractor = createCompositeKeyExtractor(sourceIdFields, schema);
      this.targetType = targetType;
      this.targetIdFields = targetIdFields;
      this.targetIdExtractor = createCompositeKeyExtractor(targetIdFields, schema);
      this.discriminators = discriminators != null ? discriminators : new HashSet<>();

      // Validate ID field types during construction
      validateIdFieldTypes(sourceIdFields, schema);
      validateIdFieldTypes(targetIdFields, schema);

      this.edges = mapper.createObjectNode();
      this.rootNode.set("edges", this.edges);
      this.attributeMappers = createAttributeMappers();
    }

    /**
     * Backwards compatibility constructor without discriminators.
     *
     * @param edgeType The type of the edge.
     * @param sourceType The type of the source vertex.
     * @param sourceIdFields List of field names for source vertex ID.
     * @param targetType The type of the target vertex.
     * @param targetIdFields List of field names for target vertex ID.
     * @param attributeMapping JSON string defining the attribute mappings.
     * @param attributeOp String defining the attribute operations.
     * @param schema The schema of the data.
     */
    public EdgeJsonBuilder(
        String edgeType,
        String sourceType,
        List<String> sourceIdFields,
        String targetType,
        List<String> targetIdFields,
        String attributeMapping,
        String attributeOp,
        StructType schema) {
      this(
          edgeType,
          sourceType,
          sourceIdFields,
          targetType,
          targetIdFields,
          attributeMapping,
          attributeOp,
          schema,
          null);
    }

    /** Clears the edges node, removing all previously added edges. */
    @Override
    public void clear() {
      edges.removeAll();
    }

    /**
     * Creates attribute mappers for edge attributes, excluding source and target ID fields.
     *
     * @return List of AttributeMapper objects for edge attributes
     */
    @Override
    protected List<AttributeMapper> createAttributeMappers() {
      Set<String> excludeFields = new HashSet<>();
      excludeFields.addAll(sourceIdFields);
      excludeFields.addAll(targetIdFields);
      return createAttributeMappers(attributeMapping, attributeOp, schema, excludeFields);
    }

    /**
     * Adds an edge object to the JSON structure based on the provided row data.
     *
     * <p>This method extracts the source and target IDs from the row and adds the edge with its
     * attributes to the JSON structure. If discriminators are specified, it handles creating arrays
     * of edges with the same source and target but different attribute values.
     *
     * <p>The method constructs a deeply nested JSON structure as follows:
     *
     * <ol>
     *   <li>First level: source vertex type
     *   <li>Second level: source vertex ID
     *   <li>Third level: edge type
     *   <li>Fourth level: target vertex type
     *   <li>Fifth level: target vertex ID (with attributes)
     * </ol>
     *
     * <p>When discriminators are used, the target ID node becomes an array of attribute objects
     * instead of a single attribute object, allowing multiple edges between the same vertices.
     *
     * @param row The data row containing values for the source ID, target ID, and attributes
     * @param attributesNode The JSON node representing the edge attributes
     */
    @Override
    protected void addObject(InternalRow row, JsonNode attributesNode) {
      String sourceId = sourceIdExtractor.apply(row);
      if (sourceId == null) {
        logger.warn("Source ID is null, skipping this row");
        return;
      }
      String targetId = targetIdExtractor.apply(row);
      if (targetId == null) {
        logger.warn("Target ID is null, skipping this row");
        return;
      }

      ObjectNode sourceVertexTypeNode = getOrCreateObjectNode(this.edges, this.sourceType);
      ObjectNode sourceVertexIdNode = getOrCreateObjectNode(sourceVertexTypeNode, sourceId);
      ObjectNode edgeTypeNode = getOrCreateObjectNode(sourceVertexIdNode, this.edgeType);
      ObjectNode targetVertexTypeNode = getOrCreateObjectNode(edgeTypeNode, this.targetType);

      // Handle discriminators
      if (discriminators.isEmpty()) {
        // No discriminators, use original logic
        targetVertexTypeNode.set(targetId, attributesNode);
      } else {
        // Check if all discriminator attributes have non-null values
        boolean allDiscriminatorsPresent = true;
        for (String discriminatorAttr : discriminators) {
          JsonNode discriminatorValue = attributesNode.path(discriminatorAttr);
          if (discriminatorValue.isMissingNode() || discriminatorValue.isNull()) {
            allDiscriminatorsPresent = false;
            logger.warn(
                "Discriminator attribute '{}' is missing or null, skipping this edge",
                discriminatorAttr);
            break;
          }
        }

        if (!allDiscriminatorsPresent) {
          // Skip the edge if any discriminator is missing
          return;
        }

        // Create target ID as JsonArray and set attributes
        JsonNode targetIdNode = targetVertexTypeNode.path(targetId);
        ArrayNode targetIdArray;
        if (targetIdNode.isMissingNode() || !targetIdNode.isArray()) {
          targetIdArray = mapper.createArrayNode();
          targetVertexTypeNode.set(targetId, targetIdArray);
        } else {
          targetIdArray = (ArrayNode) targetIdNode;
        }
        targetIdArray.add(attributesNode);
      }
    }

    /**
     * Gets or creates an ObjectNode with the given key from a parent ObjectNode.
     *
     * <p>This is a helper method for building the nested edge JSON structure.
     *
     * @param parent The parent ObjectNode
     * @param key The key to get or create
     * @return The existing or newly created ObjectNode
     */
    private static ObjectNode getOrCreateObjectNode(ObjectNode parent, String key) {
      JsonNode node = parent.path(key);
      if (node.isMissingNode() || !node.isObject()) {
        return parent.putObject(key);
      }
      return (ObjectNode) node;
    }
  }
}
